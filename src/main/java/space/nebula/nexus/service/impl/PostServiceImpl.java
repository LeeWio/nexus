package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostDeletedEvent;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.config.BlogDiscoveryProperties;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostRevisionKind;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.BatchDeleteRequest;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;
import space.nebula.nexus.payload.response.BlogFacetResponse;
import space.nebula.nexus.payload.response.CategoryResponse;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.PostAutosaveResponse;
import space.nebula.nexus.payload.response.PostResponse;
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
import space.nebula.nexus.service.PostRankingService;
import space.nebula.nexus.utils.RedisUtil;
import space.nebula.nexus.utils.PostContentAnalyzer;
import space.nebula.nexus.utils.SlugUtil;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Professional implementation of IPostService with Event-Driven decoupling,
 * MapStruct, and Autosave.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService {
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
	private final PostRankingService postRankingService;
	private final space.nebula.nexus.common.validator.PostValidator postValidator;
	private final BlogDiscoveryProperties discoveryProperties;

	private final space.nebula.nexus.repository.ConfigRepository configRepository;
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

		// Cleanup potential autosave data
		clearAutosaveData(request.slug());

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

		// Cleanup autosave data
		clearAutosaveData(id.toString());
		clearAutosaveData(existingPost.getSlug());

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
	@LogOperation("Submit Post For Review")
	public ApiResponse<Void> submitForReview(Long id) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.DRAFT || post.getStatus() == PostStatus.REJECTED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Only Draft or Rejected posts can be submitted for review"));

		post.setStatus(PostStatus.PENDING_REVIEW);
		post.setReviewComment(null);
		post.setReviewedAt(null);
		post.setReviewedBy(null);
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.SUBMITTED_FOR_REVIEW));
		log.info("Post '{}' submitted for review by {}", post.getTitle(),
				SecurityUtil.getCurrentUserOrThrow(userRepository).getUsername());
		return ApiResponse.success("Post submitted for review successfully", null);
	}

	@Override
	@Transactional
	@LogOperation("Withdraw Post From Review")
	public ApiResponse<Void> withdrawFromReview(Long id) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PENDING_REVIEW,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not pending review"));

		post.withdrawFromReview();
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.WITHDRAWN_FROM_REVIEW));
		log.info("Post '{}' withdrawn from review", post.getTitle());
		return ApiResponse.success("Post withdrawn from review", null);
	}

	@Override
	@Transactional
	@LogOperation("Review Post")
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<Void> reviewPost(Long id, boolean approved, String reviewComment) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PENDING_REVIEW,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not pending review"));
		if (!approved) {
			Assert.notBlank(reviewComment,
					() -> new BusinessException(BusinessCode.BAD_REQUEST, "A rejection reason is required"));
		}
		User reviewer = SecurityUtil.getCurrentUserOrThrow(userRepository);
		post.setReviewComment(StrUtil.blankToDefault(reviewComment, null));
		post.setReviewedAt(java.time.LocalDateTime.now());
		post.setReviewedBy(reviewer);

		if (approved) {
			post.publish();
			log.info("Post '{}' approved and published", post.getTitle());
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.PUBLISHED));
		} else {
			post.reject();
			// Normally, store the reviewComment in a post_audit or comment field
			log.info("Post '{}' rejected. Reason: {}", post.getTitle(), reviewComment);
		}

		postRepository.save(post);
		if (!approved) {
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.REJECTED));
		}
		return ApiResponse.success(approved ? "Post approved and published" : "Post rejected", null);
	}

	@Override
	@Transactional
	@LogOperation("Schedule Post Publication")
	public ApiResponse<Void> schedulePost(Long id, PostScheduleRequest request) {
		Assert.notNull(request,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "A publication schedule is required"));
		Assert.notNull(request.scheduledAt(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "A publication time is required"));
		Assert.isTrue(request.scheduledAt().isAfter(java.time.LocalDateTime.now()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "The publication time must be in the future"));
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PENDING_REVIEW || post.getStatus() == PostStatus.SCHEDULED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Only posts awaiting review or already scheduled can be scheduled"));

		User reviewer = SecurityUtil.getCurrentUserOrThrow(userRepository);
		post.schedule(request.scheduledAt());
		post.setReviewComment(null);
		post.setReviewedAt(java.time.LocalDateTime.now());
		post.setReviewedBy(reviewer);
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.SCHEDULED));
		log.info("Post '{}' scheduled for publication at {} by {}", post.getTitle(), request.scheduledAt(),
				reviewer.getUsername());
		return ApiResponse.success("Post publication scheduled", null);
	}

	@Override
	@Transactional
	@LogOperation("Cancel Scheduled Post Publication")
	public ApiResponse<Void> cancelScheduledPost(Long id) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.SCHEDULED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not scheduled for publication"));

		post.cancelSchedule();
		post.setReviewComment(null);
		post.setReviewedAt(null);
		post.setReviewedBy(null);
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.SCHEDULE_CANCELED));
		log.info("Scheduled publication canceled for post '{}'", post.getTitle());
		return ApiResponse.success("Scheduled publication canceled", null);
	}

	@Override
	@Transactional
	@LogOperation("Archive Published Post")
	public ApiResponse<Void> archivePost(Long id, PostArchiveRequest request) {
		Assert.notNull(request,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "An archive request is required"));
		Assert.notBlank(request.reason(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "An archive reason is required"));
		Assert.isTrue(request.reason().trim().length() <= 1000, () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"The archive reason must not exceed 1000 characters"));

		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PUBLISHED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only published posts can be archived"));
		User editor = SecurityUtil.getCurrentUserOrThrow(userRepository);
		post.archive(request.reason().trim(), editor);
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.ARCHIVED));
		log.info("Post '{}' archived by {}", post.getTitle(), editor.getUsername());
		return ApiResponse.success("Post archived", null);
	}

	@Override
	@Transactional
	@LogOperation("Restore Archived Post")
	public ApiResponse<Void> restoreArchivedPost(Long id) {
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.ARCHIVED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not archived"));

		post.restoreToDraft();
		postRepository.save(post);
		postRevisionService.saveRevision(post, PostRevisionKind.RESTORED,
				"Archived post restored for a new editing cycle");
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.RESTORED_TO_DRAFT));
		log.info("Archived post '{}' restored to draft", post.getTitle());
		return ApiResponse.success("Post restored to draft", null);
	}

	@Override
	@Transactional
	public int publishDueScheduledPosts(java.time.LocalDateTime now, int batchSize) {
		Assert.notNull(now, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Publication cutoff is required"));
		Assert.isTrue(batchSize >= 1 && batchSize <= 100, () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"Publication batch size must be between 1 and 100"));

		List<Long> duePostIds = postRepository.findDueScheduledPostIds(PostStatus.SCHEDULED, now,
				PageRequest.of(0, batchSize));
		if (duePostIds.isEmpty()) {
			return 0;
		}
		List<Post> duePosts = postRepository.findScheduledPublicationBatch(PostStatus.SCHEDULED, duePostIds).stream()
				.sorted(java.util.Comparator.comparing(Post::getScheduledAt).thenComparing(Post::getId)).toList();
		for (Post post : duePosts) {
			post.publish();
		}
		postRepository.saveAll(duePosts);
		for (Post post : duePosts) {
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.PUBLISHED));
		}
		if (!duePosts.isEmpty()) {
			log.info("Published {} scheduled posts at {}", duePosts.size(), now);
		}
		return duePosts.size();
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_POSTS, key = CacheConstants.POST_LIST_KEY, sync = true)
	public ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Pageable pageable) {
		return searchPublicPosts(categoryId, tagId, keyword, null, null, null, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable) {
		var spec = PostSpecification.filterPublicPosts(categoryId, tagId, keyword, featuredOnly, hasCover, contentType);
		Page<Post> publishedPosts = postRepository.findAll(spec, pageable);

		return ApiResponse.success(PageResult.of(publishedPosts.map(postMapper::toResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostDigestResponse>> searchPublicPostDigests(Long categoryId, Long tagId,
			String keyword, Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable) {
		var spec = PostSpecification.filterPublicPosts(categoryId, tagId, keyword, featuredOnly, hasCover, contentType);
		Page<Post> publishedPosts = postRepository.findAll(spec, pageable);
		return ApiResponse.success(PageResult.of(publishedPosts.map(postMapper::toDigestResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostDigestResponse>> retrievePublicArchive(Integer year, Integer month,
			Pageable pageable) {
		Assert.isTrue(month == null || year != null,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Archive month requires a year"));
		Assert.isTrue(month == null || (month >= 1 && month <= 12),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Archive month must be between 1 and 12"));
		Page<PostDigestResponse> archivePage = postRepository.findAll(archiveSpec(year, month), pageable)
				.map(postMapper::toDigestResponse);
		return ApiResponse.success(PageResult.of(archivePage));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_POSTS, key = "'facets'", sync = true)
	public ApiResponse<BlogFacetResponse> retrievePublicFacets() {
		long totalPublishedCount = postRepository.countByStatus(PostStatus.PUBLISHED);
		List<BlogFacetResponse.CategoryFacet> categories = postRepository
				.countPublishedPostsByCategory(PostStatus.PUBLISHED).stream()
				.map(row -> new BlogFacetResponse.CategoryFacet(asLong(row[0]), (String) row[1], (String) row[2],
						asLong(row[3])))
				.toList();
		List<BlogFacetResponse.TagFacet> tags = postRepository.countPublishedPostsByTag(PostStatus.PUBLISHED).stream()
				.map(row -> new BlogFacetResponse.TagFacet(asLong(row[0]), (String) row[1], (String) row[2],
						asLong(row[3])))
				.toList();
		List<BlogFacetResponse.ArchiveFacet> archives = postRepository
				.countPublishedPostsByArchiveMonth(PostStatus.PUBLISHED).stream()
				.map(row -> new BlogFacetResponse.ArchiveFacet(asInt(row[0]), asInt(row[1]), asLong(row[2]))).toList();
		List<BlogFacetResponse.ContentTypeFacet> contentTypes = postRepository
				.countPublishedPostsByContentType(PostStatus.PUBLISHED).stream()
				.map(row -> new BlogFacetResponse.ContentTypeFacet((PostContentType) row[0], asLong(row[1]))).toList();
		return ApiResponse
				.success(new BlogFacetResponse(totalPublishedCount, categories, tags, archives, contentTypes));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_POSTS, key = "'featured-' + #pageable.pageNumber + '-' + #pageable.pageSize", sync = true)
	public ApiResponse<PageResult<PostDigestResponse>> retrieveFeaturedPublicPosts(Pageable pageable) {
		Page<PostDigestResponse> page = postRepository.findProminentPublicPosts(PostStatus.PUBLISHED, pageable)
				.map(postMapper::toDigestResponse);
		return ApiResponse.success(PageResult.of(page));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_POSTS, key = CacheConstants.BLOG_DISCOVERY_KEY, sync = true)
	public ApiResponse<BlogDiscoveryResponse> retrievePublicDiscovery() {
		List<Post> discoveryCandidates = postRepository.findDiscoveryCandidates(PostStatus.PUBLISHED,
				PageRequest.of(0, candidateSize()));
		List<Post> scoredCandidates = discoveryCandidates.stream()
				.sorted(Comparator.comparingDouble(postRankingService::discoveryScore).reversed()
						.thenComparing(Post::getId, Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();

		Sort latestFirst = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
		List<Post> latestCandidates = postRepository
				.findAllByStatus(PostStatus.PUBLISHED, PageRequest.of(0, candidateSize(), latestFirst)).getContent();

		Post spotlight = postRepository
				.findAllByStatusAndIsFeaturedTrue(PostStatus.PUBLISHED, PageRequest.of(0, 1, latestFirst)).stream()
				.findFirst().orElseGet(() -> scoredCandidates.stream().findFirst().orElse(null));

		Set<Long> selectedPostIds = new LinkedHashSet<>();
		if (spotlight != null) {
			selectedPostIds.add(spotlight.getId());
		}

		List<PostDigestResponse> curated = selectDistinctDigests(scoredCandidates, selectedPostIds, sectionSize());
		List<PostDigestResponse> latest = selectDistinctDigests(latestCandidates, selectedPostIds, sectionSize());

		Sort mostReadFirst = Sort.by(Sort.Order.desc("views"), Sort.Order.desc("likesCount"),
				Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
		List<Post> mostReadCandidates = postRepository
				.findAllByStatus(PostStatus.PUBLISHED, PageRequest.of(0, candidateSize(), mostReadFirst)).getContent();
		List<PostDigestResponse> mostRead = selectDistinctDigests(mostReadCandidates, selectedPostIds, sectionSize());

		PostDigestResponse spotlightResponse = spotlight == null ? null : postMapper.toDigestResponse(spotlight);
		return ApiResponse.success(new BlogDiscoveryResponse(spotlightResponse, curated, latest, mostRead,
				buildCategoryGroups(scoredCandidates, selectedPostIds)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PostResponse> retrievePostBySlug(String slug) {
		String cacheKey = CacheConstants.POST_SLUG_PREFIX + slug;
		PostResponse postResponse = redisUtil.get(cacheKey, PostResponse.class).orElseGet(() -> {
			Post post = postRepository.findBySlug(slug)
					.orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));

			Assert.isTrue(post.isPublished(),
					() -> new BusinessException(BusinessCode.FORBIDDEN, "Post is not published"));
			PostResponse mappedResponse = postMapper.toResponse(post);
			redisUtil.set(cacheKey, mappedResponse, 1, java.util.concurrent.TimeUnit.HOURS);
			return mappedResponse;
		});

		// Async view count increment using Hash for easier batch sync
		redisUtil.hashIncrement(CacheConstants.POST_VIEW_EXTRA_HASH, postResponse.id().toString(), 1L);

		// Populate real-time and interaction data
		PostResponse.PostResponseBuilder responseBuilder = postResponse.toBuilder();
		responseBuilder.views(mergeRealtimeViews(postResponse.id(), postResponse.views()));
		interactionService.populateInteractionData(responseBuilder, postResponse.id());

		// Populate Wiki-specific metadata
		populateWikiMetadata(responseBuilder, postResponse);

		return ApiResponse.success(responseBuilder.build());
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_POSTS, key = "'related:' + #slug + ':' + #pageable.pageNumber + '-' + #pageable.pageSize", sync = true)
	public ApiResponse<List<PostDigestResponse>> retrieveRelatedPosts(String slug, Pageable pageable) {
		Post source = postRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));
		Assert.isTrue(source.isPublished(),
				() -> new BusinessException(BusinessCode.FORBIDDEN, "Post is not published"));

		int requestedSize = pageable.isPaged() ? pageable.getPageSize() : sectionSize();
		int candidateLimit = Math.max(requestedSize * 6, candidateSize());
		Pageable candidatePage = PageRequest.of(0, candidateLimit);
		List<PostDigestResponse> relatedPosts = postRepository.findAll(relatedPostsSpec(source), candidatePage)
				.getContent().stream()
				.sorted(Comparator
						.comparingDouble((Post candidate) -> postRankingService.relatedScore(source, candidate))
						.reversed().thenComparing(Post::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(Post::getId, Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(requestedSize).map(postMapper::toDigestResponse).toList();
		return ApiResponse.success(relatedPosts);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<String> createPreviewToken(Long id) {
		Post post = findPostOrThrow(id);
		String token = UUID.randomUUID().toString();
		String previewKey = CacheConstants.POST_PREVIEW_PREFIX + token;
		PostPreviewToken previewToken = new PostPreviewToken(post.getId(), previewContentHash(post), post.getStatus(),
				post.getLockVersion());
		if (!redisUtil.set(previewKey, previewToken, 30, TimeUnit.MINUTES)) {
			throw new BusinessException(BusinessCode.ERROR, "Preview service is temporarily unavailable");
		}
		return ApiResponse.success("Preview token created", token);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PostResponse> retrievePostPreview(String token) {
		Assert.notBlank(token, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Preview token is required"));
		String previewKey = CacheConstants.POST_PREVIEW_PREFIX + token;
		PostPreviewToken previewToken = redisUtil.get(previewKey, PostPreviewToken.class).orElseThrow(
				() -> new BusinessException(BusinessCode.NOT_FOUND, "Preview token is invalid or expired"));
		Post post = findPostOrThrow(previewToken.postId());
		if (!previewToken.matches(post.getId(), previewContentHash(post), post.getStatus(), post.getLockVersion())) {
			redisUtil.delete(previewKey);
			throw new BusinessException(BusinessCode.NOT_FOUND, "Preview token is invalid or expired");
		}
		PostResponse response = postMapper.toResponse(post);
		PostResponse.PostResponseBuilder responseBuilder = response.toBuilder();
		populateWikiMetadata(responseBuilder, response);
		return ApiResponse.success(responseBuilder.build());
	}

	@Override
	@Transactional
	@org.springframework.cache.annotation.CacheEvict(value = {CacheConstants.BLOG_POSTS,
			CacheConstants.SEO}, allEntries = true)
	public ApiResponse<Integer> rebuildPostContentMetadata() {
		List<Post> posts = postRepository.findAll();
		for (Post post : posts) {
			refreshContentMetadata(post);
		}
		postRepository.saveAll(posts);
		return ApiResponse.success("Post content metadata rebuilt", posts.size());
	}

	private void populateWikiMetadata(PostResponse.PostResponseBuilder builder, PostResponse post) {
		// 1. Build Breadcrumbs
		if (StrUtil.isNotBlank(post.path())) {
			String[] parts = post.path().split("/");
			java.util.List<String> ancestorPaths = new java.util.ArrayList<>();
			StringBuilder sb = new StringBuilder();
			for (String part : parts) {
				if (StrUtil.isNotBlank(part)) {
					sb.append("/").append(part).append("/");
					ancestorPaths.add(sb.toString());
				}
			}

			if (!ancestorPaths.isEmpty()) {
				var ancestors = postRepository.findByPathInOrderByPathAsc(ancestorPaths);
				var breadcrumbs = ancestors.stream()
						.map(p -> new PostResponse.Breadcrumb(p.getId(), p.getTitle(), p.getSlug()))
						.collect(Collectors.toList());
				builder.breadcrumbs(breadcrumbs);
			}
		}

		// 2. Build Navigation (Prev/Next)
		if (post.series() != null) {
			Long seriesId = post.series().id();
			Integer currentOrder = post.seriesOrder();

			var prev = postRepository.findPreviousInSeries(seriesId, currentOrder);
			var next = postRepository.findNextInSeries(seriesId, currentOrder);

			PostResponse.Navigation navigation = new PostResponse.Navigation(
					prev.map(p -> new PostResponse.Navigation.Neighbor(p.getTitle(), p.getSlug())).orElse(null),
					next.map(p -> new PostResponse.Navigation.Neighbor(p.getTitle(), p.getSlug())).orElse(null));
			builder.navigation(navigation);
		}

		// 3. Build SEO Metadata
		String siteUrl = configRepository.findByConfigKey("site_url")
				.map(space.nebula.nexus.entity.Config::getConfigValue).orElse("http://localhost:3000");
		String fullUrl = siteUrl + "/post/" + post.slug();

		PostResponse.SeoMetadata seo = new PostResponse.SeoMetadata(post.title(),
				StrUtil.blankToDefault(post.summary(), post.autoSummary()), post.coverImage(), "article", fullUrl,
				"summary_large_image", fullUrl);
		builder.seo(seo);
	}

	@Override
	public ApiResponse<Void> autosavePostContent(PostAutosaveRequest request) {
		String autosaveKey = autosaveKey(request.identifier());
		PostAutosaveResponse autosaveData = new PostAutosaveResponse(request.content(), request.contentType());
		redisUtil.set(autosaveKey, autosaveData, 24, TimeUnit.HOURS);
		log.debug("Autosaved content for identifier: {}", request.identifier());
		return ApiResponse.success("Content autosaved.", null);
	}

	@Override
	public ApiResponse<PostAutosaveResponse> retrieveAutosavedContent(String identifier) {
		String autosaveKey = autosaveKey(identifier);
		return redisUtil.get(autosaveKey, PostAutosaveResponse.class).map(ApiResponse::success).orElse(ApiResponse
				.error(BusinessCode.NOT_FOUND.getCode(), "No autosaved content was found for this identifier"));
	}

	// --- Private Helper Methods ---

	private Long mergeRealtimeViews(Long postId, Long dbViews) {
		Long totalViews = dbViews != null ? dbViews : 0L;
		Object extraViews = redisUtil.hashGet(CacheConstants.POST_VIEW_EXTRA_HASH, postId.toString());
		if (extraViews instanceof Number extraViewCount) {
			totalViews += extraViewCount.longValue();
		}
		return totalViews;
	}

	private List<PostDigestResponse> selectDistinctDigests(List<Post> candidates, Set<Long> selectedPostIds,
			int limit) {
		return candidates.stream().filter(post -> post.getId() != null && selectedPostIds.add(post.getId()))
				.limit(limit).map(postMapper::toDigestResponse).toList();
	}

	private List<BlogDiscoveryResponse.CategoryGroup> buildCategoryGroups(List<Post> candidates,
			Set<Long> selectedPostIds) {
		Map<Long, List<Post>> byCategory = candidates.stream()
				.filter(post -> post.getId() != null && !selectedPostIds.contains(post.getId()))
				.filter(post -> post.getCategory() != null && post.getCategory().getId() != null)
				.collect(Collectors.groupingBy(post -> post.getCategory().getId()));
		List<BlogDiscoveryResponse.CategoryGroup> groups = new ArrayList<>();
		for (List<Post> posts : byCategory.values()) {
			List<Post> rankedPosts = posts.stream()
					.sorted(Comparator.comparingDouble(postRankingService::discoveryScore).reversed())
					.limit(categoryPostSize()).toList();
			if (rankedPosts.isEmpty()) {
				continue;
			}
			double score = rankedPosts.stream().mapToDouble(postRankingService::discoveryScore).sum();
			groups.add(new BlogDiscoveryResponse.CategoryGroup(toCategoryResponse(rankedPosts.getFirst()),
					rankedPosts.stream().map(postMapper::toDigestResponse).toList(), score));
		}
		return groups.stream().sorted(Comparator.comparingDouble(BlogDiscoveryResponse.CategoryGroup::score).reversed())
				.limit(categoryGroupSize()).toList();
	}

	private CategoryResponse toCategoryResponse(Post post) {
		var category = post.getCategory();
		return new CategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription(),
				category.getIcon(), category.getCreatedAt());
	}

	private int sectionSize() {
		return positive(discoveryProperties.getSectionSize(), 6);
	}

	private int categoryGroupSize() {
		return positive(discoveryProperties.getCategoryGroupSize(), 3);
	}

	private int categoryPostSize() {
		return positive(discoveryProperties.getCategoryPostSize(), 4);
	}

	private int candidateSize() {
		return positive(discoveryProperties.getCandidateSize(), 48);
	}

	private int positive(int configured, int fallback) {
		return configured > 0 ? configured : fallback;
	}

	private Specification<Post> relatedPostsSpec(Post source) {
		return (root, query, cb) -> {
			query.distinct(true);
			List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("status"), PostStatus.PUBLISHED));
			predicates.add(cb.notEqual(root.get("id"), source.getId()));

			List<jakarta.persistence.criteria.Predicate> relationPredicates = new ArrayList<>();
			if (source.getCategory() != null && source.getCategory().getId() != null) {
				relationPredicates.add(cb.equal(root.get("category").get("id"), source.getCategory().getId()));
			}
			if (source.getSeries() != null && source.getSeries().getId() != null) {
				relationPredicates.add(cb.equal(root.get("series").get("id"), source.getSeries().getId()));
			}
			if (source.getContentType() != null) {
				relationPredicates.add(cb.equal(root.get("contentType"), source.getContentType()));
			}
			Set<Long> tagIds = source.getTags().stream().filter(tag -> tag.getId() != null)
					.map(space.nebula.nexus.entity.Tag::getId).collect(Collectors.toSet());
			if (!tagIds.isEmpty()) {
				var tagJoin = root.join("tags", jakarta.persistence.criteria.JoinType.LEFT);
				relationPredicates.add(tagJoin.get("id").in(tagIds));
			}
			if (!relationPredicates.isEmpty()) {
				predicates.add(cb.or(relationPredicates.toArray(jakarta.persistence.criteria.Predicate[]::new)));
			}
			return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private Specification<Post> archiveSpec(Integer year, Integer month) {
		return (root, query, cb) -> {
			List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
			predicates.add(cb.equal(root.get("status"), PostStatus.PUBLISHED));
			predicates.add(cb.isNotNull(root.get("publishedAt")));
			if (year != null) {
				predicates.add(cb.equal(cb.function("year", Integer.class, root.get("publishedAt")), year));
			}
			if (month != null) {
				predicates.add(cb.equal(cb.function("month", Integer.class, root.get("publishedAt")), month));
			}
			return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
	}

	private Long asLong(Object value) {
		return value instanceof Number number ? number.longValue() : 0L;
	}

	private int asInt(Object value) {
		return value instanceof Number number ? number.intValue() : 0;
	}

	private void clearAutosaveData(String identifier) {
		if (identifier != null) {
			redisUtil.delete(autosaveKey(identifier));
		}
	}

	private String autosaveKey(String identifier) {
		String username = SecurityUtil.getCurrentUsername();
		Assert.notBlank(username, () -> new BusinessException(BusinessCode.UNAUTHORIZED, "Authentication required"));
		return CacheConstants.POST_AUTOSAVE_PREFIX + username + ":" + identifier;
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
		String suffix = "-copy-" + UUID.randomUUID().toString().substring(0, 8);
		String sourceSlug = StrUtil.blankToDefault(source.getSlug(), SlugUtil.toSlug(source.getTitle()));
		int maxBaseLength = 200 - suffix.length();
		String requestedSlug = sourceSlug.length() <= maxBaseLength
				? sourceSlug + suffix
				: sourceSlug.substring(0, maxBaseLength) + suffix;
		return slugService.generateUniqueSlug(requestedSlug, copiedTitle,
				candidate -> postRepository.findBySlug(candidate).isPresent());
	}

	private String validateAndGenerateSlug(String requestedSlug, String title) {
		String slug = StrUtil.isBlank(requestedSlug) ? SlugUtil.toSlug(title) : SlugUtil.toSlug(requestedSlug);
		Assert.isFalse(postRepository.findBySlug(slug).isPresent(),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Post slug is already in use: " + slug));
		return slug;
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
		// Sync Category
		if (request.categoryId() != null) {
			post.setCategory(categoryRepository.findById(request.categoryId())
					.orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId())));
		} else {
			post.setCategory(null);
		}

		// Sync Series
		if (request.seriesId() != null) {
			post.setSeries(seriesRepository.findById(request.seriesId())
					.orElseThrow(() -> new ResourceNotFoundException("Series", "id", request.seriesId())));
			post.setSeriesOrder(request.seriesOrder() != null ? request.seriesOrder() : 0);
		} else {
			post.setSeries(null);
			post.setSeriesOrder(0);
		}

		// Sync Tags - Fix: handle empty/null tagIds correctly to clear existing tags
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
		post.setWordCount(metadata.wordCount());
		post.setReadingTimeMinutes(metadata.readingTimeMinutes());
		post.setAutoSummary(metadata.autoSummary());
		post.setToc(metadata.toc());
		post.setContentHash(metadata.contentHash());
	}

	private String previewContentHash(Post post) {
		if (StrUtil.isNotBlank(post.getContentHash())) {
			return post.getContentHash();
		}
		return PostContentAnalyzer.analyze(post.getTitle(), post.getSummary(), post.getContent(), post.getContentType())
				.contentHash();
	}
}
