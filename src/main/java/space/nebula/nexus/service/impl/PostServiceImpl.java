package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostDeletedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostRevisionKind;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.BatchDeleteRequest;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostMetadataRebuildResponse;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PostSeriesRepository;
import space.nebula.nexus.repository.TagRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.specification.PostSpecification;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.service.IPostRevisionService;
import space.nebula.nexus.service.IPostService;
import space.nebula.nexus.service.ISlugService;
import space.nebula.nexus.service.PostEditorDraftService;
import space.nebula.nexus.utils.RedisUtil;
import space.nebula.nexus.utils.PostContentAnalyzer;
import space.nebula.nexus.utils.SlugUtil;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles editorial post commands and database-backed editor drafts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {
	@PersistenceContext
	private EntityManager entityManager;
	private final PostRepository postRepository;
	private final CategoryRepository categoryRepository;
	private final TagRepository tagRepository;
	private final PostSeriesRepository seriesRepository;
	private final UserRepository userRepository;
	private final PostMapper postMapper;
	private final RedisUtil redisUtil;
	private final ApplicationEventPublisher eventPublisher;
	private final IInteractionService interactionService;
	private final IPostRevisionService postRevisionService;
	private final ISlugService slugService;
	private final space.nebula.nexus.common.validator.PostValidator postValidator;
	private final PostEditorDraftService postEditorDraftService;
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostResponse>> searchPostsForAdmin(PostStatus status, Long categoryId, String keyword,
			Pageable pageable) {
		var spec = PostSpecification.filterPosts(status, categoryId, null, keyword);
		Page<Post> adminPostPage = postRepository.findAll(spec, pageable);
		return ApiResponse.success(PageResult.of(adminPostPage.map(postMapper::toResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PostResponse> retrievePostById(Long id) {
		Post post = findPostOrThrow(id);

		PostResponse.PostResponseBuilder responseBuilder = postMapper.toResponse(post).toBuilder();
		interactionService.populateInteractionData(responseBuilder, id);

		// Merge real-time views from Redis hash
		responseBuilder.views(mergeRealtimeViews(post.getId(), post.getViews()));

		return ApiResponse.success(responseBuilder.build());
	}

	@Override
	@Transactional
	@LogOperation("Create Blog Post")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<PostResponse> createPost(PostRequest request) {
		return createPost(request, null);
	}

	@Override
	@Transactional
	@LogOperation("Create Blog Post")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<PostResponse> createPost(PostRequest request, String editorDraftRevision) {
		postValidator.validatePostRequest(request);
		String uniqueSlug = slugService.generateUniqueSlug(request.slug(), request.title(),
				s -> postRepository.findBySlug(s).isPresent());
		User currentAuthor = SecurityUtil.getCurrentUserOrThrow(userRepository);

		Post newPost = new Post();
		postMapper.updateEntity(newPost, request);
		newPost.setSlug(uniqueSlug);
		newPost.setAuthor(currentAuthor);

		// Administrators can directly control the workflow state from the payload.
		// Standard users are restricted to DRAFT until dedicated commands are invoked.
		if (SecurityUtil.hasRole("ADMIN") && request.status() != null) {
			newPost.setStatus(request.status());
			if (request.status() == PostStatus.PUBLISHED && newPost.getPublishedAt() == null) {
				newPost.setPublishedAt(java.time.LocalDateTime.now());
			}
		} else {
			newPost.moveToDraft();
			newPost.setIsFeatured(false);
		}

		if (request.contentType() != null) {
			newPost.setContentType(request.contentType());
		}
		refreshContentMetadata(newPost);

		syncCategoryAndTags(newPost, request);
		syncParentPost(newPost, request);

		postRepository.save(newPost);

		// If it's a new post or path was not set, update path after ID is assigned
		if (newPost.getPath() == null) {
			newPost.updatePath(newPost.getParent());
			postRepository.save(newPost);
		}
		postRevisionService.saveRevision(newPost, PostRevisionKind.CREATED, "Initial post content");

		log.info("Blog post created: {} by {}", newPost.getTitle(), currentAuthor.getUsername());

		if (editorDraftRevision != null) {
			postEditorDraftService.consume(currentAuthor.getId(), PostEditorDraftService.NEW_ARTICLE,
					editorDraftRevision);
		}

		// Publish event
		eventPublisher.publishEvent(new PostChangedEvent(this, newPost, PostChangeType.CREATED));

		return ApiResponse.success("Post created successfully", postMapper.toResponse(newPost));
	}

	@Override
	@Transactional
	@LogOperation("Update Blog Post")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<PostResponse> updatePost(Long id, PostRequest request) {
		return updatePost(id, request, null);
	}

	@Override
	@Transactional
	@LogOperation("Update Blog Post")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<PostResponse> updatePost(Long id, PostRequest request, Integer expectedRevisionNumber) {
		return updatePost(id, request, expectedRevisionNumber, null);
	}

	@Override
	@Transactional
	@LogOperation("Update Blog Post")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<PostResponse> updatePost(Long id, PostRequest request, Integer expectedRevisionNumber,
			String editorDraftRevision) {
		postValidator.validatePostRequest(request);
		Post existingPost = findPostForUpdateOrThrow(id);
		postRevisionService.assertExpectedRevision(id, expectedRevisionNumber);

		// Admins can edit posts in any state; standard users only DRAFT or REJECTED.
		boolean isAdmin = SecurityUtil.hasRole("ADMIN");
		if (!isAdmin) {
			Assert.isTrue(existingPost.isEditable(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
					"Only draft or rejected posts can be edited"));
		}

		String previousPath = existingPost.getPath();
		String previousSlug = existingPost.getSlug();
		PostStatus previousStatus = existingPost.getStatus();
		Boolean previousFeatured = existingPost.getIsFeatured();

		if (StrUtil.isNotBlank(request.slug()) && !StrUtil.equals(request.slug(), existingPost.getSlug())) {
			String newSlug = slugService.generateUniqueSlug(request.slug(), request.title(),
					s -> postRepository.findBySlug(s).isPresent());
			existingPost.setSlug(newSlug);
		}

		postMapper.updateEntity(existingPost, request);

		// Only administrators may change workflow or featured state through the
		// general edit payload. Other users must use the dedicated review flow.
		if (isAdmin && request.status() != null) {
			existingPost.setStatus(request.status());
			if (request.status() == PostStatus.PUBLISHED && existingPost.getPublishedAt() == null) {
				existingPost.setPublishedAt(java.time.LocalDateTime.now());
			}
		} else {
			existingPost.setStatus(previousStatus);
			existingPost.setIsFeatured(previousFeatured);
		}

		if (request.contentType() != null) {
			existingPost.setContentType(request.contentType());
		}
		refreshContentMetadata(existingPost);

		syncCategoryAndTags(existingPost, request);
		syncParentPost(existingPost, request);

		postRepository.save(existingPost);
		if (previousPath != null && !previousPath.equals(existingPost.getPath())) {
			postRepository.replaceDescendantPathPrefix(existingPost.getId(), previousPath, existingPost.getPath());
		}
		postRevisionService.saveRevision(existingPost, PostRevisionKind.UPDATED, "Post content or metadata updated");

		log.info("Blog post updated: {}", existingPost.getTitle());

		if (editorDraftRevision != null) {
			Long userId = SecurityUtil.getCurrentUserOrThrow(userRepository).getId();
			postEditorDraftService.consume(userId, id.toString(), editorDraftRevision);
		}

		// Publish event
		eventPublisher.publishEvent(
				new PostChangedEvent(this, existingPost, PostChangeType.UPDATED, previousSlug, previousStatus));

		return ApiResponse.success("Post updated successfully", postMapper.toResponse(existingPost));
	}

	@Override
	@Transactional
	@LogOperation("Delete Blog Post")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<Void> deletePost(Long id) {
		Post postToDelete = findPostForUpdateOrThrow(id);
		Assert.isTrue(postToDelete.isDeletable(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Withdraw, cancel, or archive this post before deleting it"));
		Assert.isFalse(postRepository.existsByParentId(id), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Move or delete child posts before deleting this post"));
		String currentSlug = postToDelete.getSlug();

		postRepository.delete(postToDelete);
		log.info("Blog post deleted id: {}", id);

		// Publish deletion event
		eventPublisher.publishEvent(new PostDeletedEvent(this, id, currentSlug));

		return ApiResponse.success("Post deleted successfully", null);
	}

	@Override
	@Transactional
	@LogOperation("Batch Delete Blog Posts")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<Void> deletePosts(BatchDeleteRequest request) {
		Assert.notNull(request, () -> new BusinessException(BusinessCode.BAD_REQUEST, "A delete request is required"));
		List<Long> postIds = request.ids().stream().distinct().sorted().toList();
		User currentUser = SecurityUtil.getCurrentUserOrThrow(userRepository);
		List<Post> posts = postRepository.findAllByIdInForUpdate(postIds);
		Assert.isTrue(posts.size() == postIds.size(), () -> new ResourceNotFoundException("Post", "ids", postIds));

		for (Post post : posts) {
			Assert.isTrue(canManagePost(currentUser, post), () -> new BusinessException(BusinessCode.FORBIDDEN,
					"You do not have permission to delete one or more selected posts"));
			Assert.isTrue(post.isDeletable(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
					"Withdraw, cancel, or archive every selected post before deleting it"));
		}

		Set<Long> blockedParentIds = postRepository.findParentIdsWithChildrenOutside(postIds, postIds);
		Assert.isTrue(blockedParentIds.isEmpty(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Move or include all child posts before deleting their parents"));

		postRepository.deleteAll(posts);
		posts.forEach(post -> eventPublisher.publishEvent(new PostDeletedEvent(this, post.getId(), post.getSlug())));
		log.info("Deleted {} blog posts by {}", posts.size(), currentUser.getUsername());
		return ApiResponse.success("Posts deleted successfully", null);
	}

	@Override
	@Transactional
	@LogOperation("Copy Blog Post")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<PostResponse> copyPost(Long id) {
		Post source = findPostOrThrow(id);
		User currentAuthor = SecurityUtil.getCurrentUserOrThrow(userRepository);
		String copiedTitle = copyTitle(source.getTitle());

		Post copiedPost = new Post();
		copiedPost.setTitle(copiedTitle);
		copiedPost.setSlug(generateCopySlug(source, copiedTitle));
		copiedPost.setCoverImage(source.getCoverImage());
		copiedPost.setShaderPreset(source.getShaderPreset());
		copiedPost.setSummary(source.getSummary());
		copiedPost.setContent(source.getContent());
		copiedPost.setContentType(source.getContentType());
		copiedPost.moveToDraft();
		copiedPost.setIsFeatured(false);
		copiedPost.setCategory(source.getCategory());
		copiedPost.setTags(source.getTags() == null ? new HashSet<>() : new HashSet<>(source.getTags()));
		copiedPost.setAuthor(currentAuthor);
		copiedPost.setSeries(null);
		copiedPost.setSeriesOrder(0);
		copiedPost.setParent(null);
		copiedPost.setViews(0L);
		copiedPost.setLikesCount(0L);
		copiedPost.setFavoritesCount(0L);
		copiedPost.setPublishedAt(null);
		copiedPost.setScheduledAt(null);
		copiedPost.setReviewComment(null);
		copiedPost.setReviewedAt(null);
		copiedPost.setReviewedBy(null);
		copiedPost.setArchiveReason(null);
		copiedPost.setArchivedAt(null);
		copiedPost.setArchivedBy(null);
		refreshContentMetadata(copiedPost);

		postRepository.save(copiedPost);
		copiedPost.updatePath(null);
		postRepository.save(copiedPost);
		postRevisionService.saveRevision(copiedPost, PostRevisionKind.CREATED, "Copied from post " + source.getId());
		eventPublisher.publishEvent(new PostChangedEvent(this, copiedPost, PostChangeType.CREATED));
		log.info("Post {} copied as {} by {}", source.getId(), copiedPost.getId(), currentAuthor.getUsername());
		return ApiResponse.success("Post copied successfully", postMapper.toResponse(copiedPost));
	}

	@Override
	@Transactional
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true, condition = "!#dryRun")
	public ApiResponse<PostMetadataRebuildResponse> rebuildPostContentMetadata(int batchSize, boolean dryRun) {
		int page = 0;
		long processed = 0;
		long updated = 0;
		Page<Post> posts;
		do {
			posts = postRepository.findAll(PageRequest.of(page, batchSize, Sort.by(Sort.Direction.ASC, "id")));
			for (Post post : posts) {
				PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze(post.getTitle(), post.getSummary(),
						post.getContent(), post.getContentType());
				if (metadataDiffers(post, metadata)) {
					updated++;
					if (!dryRun) {
						applyContentMetadata(post, metadata);
					}
				}
				processed++;
			}
			if (!dryRun && !posts.isEmpty()) {
				postRepository.saveAll(posts.getContent());
				entityManager.flush();
				entityManager.clear();
			}
			page++;
		} while (posts.hasNext());
		return ApiResponse.success("Post content metadata rebuilt",
				new PostMetadataRebuildResponse(processed, updated, dryRun));
	}

	private boolean metadataDiffers(Post post, PostContentAnalyzer.Metadata metadata) {
		return !java.util.Objects.equals(post.getWordCount(), metadata.wordCount())
				|| !java.util.Objects.equals(post.getReadingTimeMinutes(), metadata.readingTimeMinutes())
				|| !java.util.Objects.equals(post.getAutoSummary(), metadata.autoSummary())
				|| !java.util.Objects.equals(post.getToc(), metadata.toc())
				|| !java.util.Objects.equals(post.getContentHash(), metadata.contentHash());
	}

	private void applyContentMetadata(Post post, PostContentAnalyzer.Metadata metadata) {
		post.setWordCount(metadata.wordCount());
		post.setReadingTimeMinutes(metadata.readingTimeMinutes());
		post.setAutoSummary(metadata.autoSummary());
		post.setToc(metadata.toc());
		post.setContentHash(metadata.contentHash());
	}

	private Long mergeRealtimeViews(Long postId, Long dbViews) {
		Long totalViews = dbViews != null ? dbViews : 0L;
		Object extraViews = redisUtil.hashGet(CacheConstants.POST_VIEW_EXTRA_HASH, postId.toString());
		if (extraViews instanceof Number extraViewCount) {
			totalViews += extraViewCount.longValue();
		}
		return totalViews;
	}

	private Post findPostOrThrow(Long id) {
		return postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
	}

	private Post findPostForUpdateOrThrow(Long id) {
		return postRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
	}

	private boolean canManagePost(User currentUser, Post post) {
		return SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("EDITOR") || post.isAuthor(currentUser);
	}

	private String copyTitle(String sourceTitle) {
		String suffix = " (Copy)";
		int maxSourceTitleLength = 200 - suffix.length();
		return sourceTitle.length() <= maxSourceTitleLength
				? sourceTitle + suffix
				: sourceTitle.substring(0, maxSourceTitleLength) + suffix;
	}

	private String generateCopySlug(Post source, String copiedTitle) {
		String suffix = "-copy-" + java.util.UUID.randomUUID().toString().substring(0, 8);
		String sourceSlug = StrUtil.blankToDefault(source.getSlug(), SlugUtil.toSlug(source.getTitle()));
		int maxBaseLength = 200 - suffix.length();
		String requestedSlug = sourceSlug.length() <= maxBaseLength
				? sourceSlug + suffix
				: sourceSlug.substring(0, maxBaseLength) + suffix;
		return slugService.generateUniqueSlug(requestedSlug, copiedTitle,
				candidate -> postRepository.findBySlug(candidate).isPresent());
	}

	private void syncParentPost(Post post, PostRequest request) {
		if (request.parentId() != null) {
			if (post.getId() != null && request.parentId().equals(post.getId())) {
				throw new BusinessException(BusinessCode.BAD_REQUEST, "A post cannot be its own parent");
			}
			Post parent = findPostOrThrow(request.parentId());
			if (post.getId() != null && parent.getPath() != null
					&& parent.getPath().contains("/" + post.getId() + "/")) {
				throw new BusinessException(BusinessCode.BAD_REQUEST,
						"A post cannot be moved below one of its descendants");
			}
			post.setParent(parent);
			post.updatePath(parent);
		} else {
			post.setParent(null);
			post.updatePath(null);
		}
	}

	private void syncCategoryAndTags(Post post, PostRequest request) {
		if (request.categoryId() != null) {
			post.setCategory(categoryRepository.findById(request.categoryId())
					.orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId())));
		} else {
			post.setCategory(null);
		}
		if (request.seriesId() != null) {
			post.setSeries(seriesRepository.findById(request.seriesId())
					.orElseThrow(() -> new ResourceNotFoundException("Series", "id", request.seriesId())));
			post.setSeriesOrder(request.seriesOrder() != null ? request.seriesOrder() : 0);
		} else {
			post.setSeries(null);
			post.setSeriesOrder(0);
		}
		if (request.tagIds() != null) {
			if (request.tagIds().isEmpty()) {
				post.setTags(new HashSet<>());
			} else {
				List<space.nebula.nexus.entity.Tag> tags = tagRepository.findAllById(request.tagIds());
				Assert.isTrue(tags.size() == request.tagIds().size(),
						() -> new BusinessException(BusinessCode.BAD_REQUEST,
								"One or more selected tags do not exist"));
				post.setTags(new HashSet<>(tags));
			}
		}
	}

	private void refreshContentMetadata(Post post) {
		PostContentAnalyzer.Metadata metadata = PostContentAnalyzer.analyze(post.getTitle(), post.getSummary(),
				post.getContent(), post.getContentType());
		applyContentMetadata(post, metadata);
	}

}
