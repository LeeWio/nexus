package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;
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
import space.nebula.nexus.service.IPostService;
import space.nebula.nexus.service.ISlugService;
import space.nebula.nexus.common.validator.PostValidator;
import space.nebula.nexus.utils.RedisUtil;
import space.nebula.nexus.utils.SlugUtil;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Professional implementation of IPostService with Event-Driven decoupling,
 * MapStruct, and Autosave.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements IPostService
{
	private static final int DISCOVERY_SECTION_SIZE = 6;
	private static final int DISCOVERY_CANDIDATE_SIZE = 20;

	private final PostRepository postRepository;
	private final CategoryRepository categoryRepository;
	private final TagRepository tagRepository;
	private final PostSeriesRepository seriesRepository;
	private final UserRepository userRepository;
	private final PostMapper postMapper;
	private final RedisUtil redisUtil;
	private final ApplicationEventPublisher eventPublisher;
	private final IInteractionService interactionService;
	private final ISlugService slugService;
	private final space.nebula.nexus.common.validator.PostValidator postValidator;

	private final space.nebula.nexus.repository.ConfigRepository configRepository;
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostResponse>> searchPostsForAdmin(PostStatus status, Long categoryId, String keyword, Pageable pageable)
	{
		var spec = PostSpecification.filterPosts(status, categoryId, null, keyword);
		Page<Post> adminPostPage = postRepository.findAll(spec, pageable);
		return ApiResponse.success(PageResult.of(adminPostPage.map(postMapper::toResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PostResponse> retrievePostById(Long id)
	{
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
	@org.springframework.cache.annotation.CacheEvict(value = { CacheConstants.BLOG_POSTS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<PostResponse> createPost(PostRequest request)
	{
		postValidator.validatePostRequest(request);
		String uniqueSlug = slugService.generateUniqueSlug(request.slug(), request.title(),
				s -> postRepository.findBySlug(s).isPresent());
		User currentAuthor = SecurityUtil.getCurrentUserOrThrow(userRepository);

		Post newPost = new Post();
		postMapper.updateEntity(newPost, request);
		newPost.setSlug(uniqueSlug);
		newPost.setAuthor(currentAuthor);
		// Workflow state is controlled by dedicated commands, never by CRUD payloads.
		newPost.moveToDraft();

		if (request.contentType() != null)
		{
			newPost.setContentType(request.contentType());
		}

		syncCategoryAndTags(newPost, request);
		syncParentPost(newPost, request);

		postRepository.save(newPost);

		// If it's a new post or path was not set, update path after ID is assigned
		if (newPost.getPath() == null)
		{
			newPost.updatePath(newPost.getParent());
			postRepository.save(newPost);
		}

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
	@org.springframework.cache.annotation.CacheEvict(value = { CacheConstants.BLOG_POSTS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<PostResponse> updatePost(Long id, PostRequest request)
	{
		postValidator.validatePostRequest(request);
		Post existingPost = findPostForUpdateOrThrow(id);
		Assert.isTrue(existingPost.isEditable(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Only draft or rejected posts can be edited"));
		String previousPath = existingPost.getPath();
		String previousSlug = existingPost.getSlug();

		if (StrUtil.isNotBlank(request.slug()) && !StrUtil.equals(request.slug(), existingPost.getSlug()))
		{
			String newSlug = slugService.generateUniqueSlug(request.slug(), request.title(),
					s -> postRepository.findBySlug(s).isPresent());
			existingPost.setSlug(newSlug);
		}

		postMapper.updateEntity(existingPost, request);

		syncCategoryAndTags(existingPost, request);
		syncParentPost(existingPost, request);

		postRepository.save(existingPost);
		if (previousPath != null && !previousPath.equals(existingPost.getPath()))
		{
			postRepository.replaceDescendantPathPrefix(existingPost.getId(), previousPath, existingPost.getPath());
		}

		log.info("Blog post updated: {}", existingPost.getTitle());

		// Cleanup autosave data
		clearAutosaveData(id.toString());
		clearAutosaveData(existingPost.getSlug());

		// Publish event
		eventPublisher.publishEvent(new PostChangedEvent(this, existingPost, PostChangeType.UPDATED, previousSlug));

		return ApiResponse.success("Post updated successfully", postMapper.toResponse(existingPost));
	}

	@Override
	@Transactional
	@LogOperation("Delete Blog Post")
	@org.springframework.cache.annotation.CacheEvict(value = { CacheConstants.BLOG_POSTS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<Void> deletePost(Long id)
	{
		Post postToDelete = findPostForUpdateOrThrow(id);
		Assert.isTrue(postToDelete.isDeletable(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Withdraw, cancel, or archive this post before deleting it"));
		Assert.isFalse(postRepository.existsByParentId(id),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
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
	@LogOperation("Submit Post For Review")
	public ApiResponse<Void> submitForReview(Long id)
	{
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
	public ApiResponse<Void> withdrawFromReview(Long id)
	{
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
	@org.springframework.cache.annotation.CacheEvict(value = { CacheConstants.BLOG_POSTS, CacheConstants.SEO }, allEntries = true)
	public ApiResponse<Void> reviewPost(Long id, boolean approved, String reviewComment)
	{
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.PENDING_REVIEW,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not pending review"));
		if (!approved)
		{
			Assert.notBlank(reviewComment,
					() -> new BusinessException(BusinessCode.BAD_REQUEST,
							"A rejection reason is required"));
		}
		User reviewer = SecurityUtil.getCurrentUserOrThrow(userRepository);
		post.setReviewComment(StrUtil.blankToDefault(reviewComment, null));
		post.setReviewedAt(java.time.LocalDateTime.now());
		post.setReviewedBy(reviewer);

		if (approved)
		{
			post.publish();
			log.info("Post '{}' approved and published", post.getTitle());
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.PUBLISHED));
		}
		else
		{
			post.reject();
			// Normally, store the reviewComment in a post_audit or comment field
			log.info("Post '{}' rejected. Reason: {}", post.getTitle(), reviewComment);
		}

		postRepository.save(post);
		if (!approved)
		{
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.REJECTED));
		}
		return ApiResponse.success(approved ? "Post approved and published" : "Post rejected", null);
	}

	@Override
	@Transactional
	@LogOperation("Schedule Post Publication")
	public ApiResponse<Void> schedulePost(Long id, PostScheduleRequest request)
	{
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
	public ApiResponse<Void> cancelScheduledPost(Long id)
	{
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
	public ApiResponse<Void> archivePost(Long id, PostArchiveRequest request)
	{
		Assert.notNull(request,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "An archive request is required"));
		Assert.notBlank(request.reason(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "An archive reason is required"));
		Assert.isTrue(request.reason().trim().length() <= 1000,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
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
	public ApiResponse<Void> restoreArchivedPost(Long id)
	{
		Post post = findPostForUpdateOrThrow(id);
		Assert.isTrue(post.getStatus() == PostStatus.ARCHIVED,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Post is not archived"));

		post.restoreToDraft();
		postRepository.save(post);
		eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.RESTORED_TO_DRAFT));
		log.info("Archived post '{}' restored to draft", post.getTitle());
		return ApiResponse.success("Post restored to draft", null);
	}

	@Override
	@Transactional
	public int publishDueScheduledPosts(java.time.LocalDateTime now, int batchSize)
	{
		Assert.notNull(now, () -> new BusinessException(BusinessCode.BAD_REQUEST, "Publication cutoff is required"));
		Assert.isTrue(batchSize >= 1 && batchSize <= 100,
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Publication batch size must be between 1 and 100"));

		List<Long> duePostIds = postRepository.findDueScheduledPostIds(PostStatus.SCHEDULED, now,
				PageRequest.of(0, batchSize));
		if (duePostIds.isEmpty())
		{
			return 0;
		}
		List<Post> duePosts = postRepository.findScheduledPublicationBatch(PostStatus.SCHEDULED, duePostIds).stream()
				.sorted(java.util.Comparator.comparing(Post::getScheduledAt).thenComparing(Post::getId)).toList();
		for (Post post : duePosts)
		{
			post.publish();
		}
		postRepository.saveAll(duePosts);
		for (Post post : duePosts)
		{
			eventPublisher.publishEvent(new PostChangedEvent(this, post, PostChangeType.PUBLISHED));
		}
		if (!duePosts.isEmpty())
		{
			log.info("Published {} scheduled posts at {}", duePosts.size(), now);
		}
		return duePosts.size();
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_POSTS, key = CacheConstants.POST_LIST_KEY, sync = true)
	public ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Pageable pageable)
	{
		var spec = PostSpecification.filterPublicPosts(categoryId, tagId, keyword);
		Page<Post> publishedPosts = postRepository.findAll(spec, pageable);

		return ApiResponse.success(PageResult.of(publishedPosts.map(postMapper::toResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_POSTS, key = CacheConstants.BLOG_DISCOVERY_KEY, sync = true)
	public ApiResponse<BlogDiscoveryResponse> retrievePublicDiscovery()
	{
		Sort latestFirst = Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
		List<Post> latestCandidates = postRepository.findAllByStatus(PostStatus.PUBLISHED,
				PageRequest.of(0, DISCOVERY_CANDIDATE_SIZE, latestFirst)).getContent();

		Post spotlight = postRepository.findAllByStatusAndIsFeaturedTrue(PostStatus.PUBLISHED,
				PageRequest.of(0, 1, latestFirst)).stream().findFirst()
				.orElseGet(() -> latestCandidates.stream().findFirst().orElse(null));

		Set<Long> selectedPostIds = new LinkedHashSet<>();
		if (spotlight != null)
		{
			selectedPostIds.add(spotlight.getId());
		}

		List<PostDigestResponse> latest = selectDistinctDigests(latestCandidates, selectedPostIds,
				DISCOVERY_SECTION_SIZE);

		Sort mostReadFirst = Sort.by(Sort.Order.desc("views"), Sort.Order.desc("likesCount"),
				Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
		List<Post> mostReadCandidates = postRepository.findAllByStatus(PostStatus.PUBLISHED,
				PageRequest.of(0, DISCOVERY_CANDIDATE_SIZE, mostReadFirst)).getContent();
		List<PostDigestResponse> mostRead = selectDistinctDigests(mostReadCandidates, selectedPostIds,
				DISCOVERY_SECTION_SIZE);

		PostDigestResponse spotlightResponse = spotlight == null ? null : postMapper.toDigestResponse(spotlight);
		return ApiResponse.success(new BlogDiscoveryResponse(spotlightResponse, latest, mostRead));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PostResponse> retrievePostBySlug(String slug)
	{
		String cacheKey = CacheConstants.POST_SLUG_PREFIX + slug;
		PostResponse postResponse = redisUtil.get(cacheKey, PostResponse.class).orElseGet(() ->
		{
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

	private void populateWikiMetadata(PostResponse.PostResponseBuilder builder, PostResponse post)
	{
		// 1. Build Breadcrumbs
		if (StrUtil.isNotBlank(post.path()))
		{
			String[] parts = post.path().split("/");
			java.util.List<String> ancestorPaths = new java.util.ArrayList<>();
			StringBuilder sb = new StringBuilder();
			for (String part : parts)
			{
				if (StrUtil.isNotBlank(part))
				{
					sb.append("/").append(part).append("/");
					ancestorPaths.add(sb.toString());
				}
			}

			if (!ancestorPaths.isEmpty())
			{
				var ancestors = postRepository.findByPathInOrderByPathAsc(ancestorPaths);
				var breadcrumbs = ancestors.stream()
						.map(p -> new PostResponse.Breadcrumb(p.getId(), p.getTitle(), p.getSlug()))
						.collect(Collectors.toList());
				builder.breadcrumbs(breadcrumbs);
			}
		}

		// 2. Build Navigation (Prev/Next)
		if (post.series() != null)
		{
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

		PostResponse.SeoMetadata seo = new PostResponse.SeoMetadata(
				post.title(),
				post.summary(),
				post.coverImage(),
				"article",
				fullUrl,
				"summary_large_image",
				fullUrl
		);
		builder.seo(seo);
	}

	@Override
	public ApiResponse<Void> autosavePostContent(PostAutosaveRequest request)
	{
		String autosaveKey = autosaveKey(request.identifier());
		PostAutosaveResponse autosaveData = new PostAutosaveResponse(request.content(), request.contentType());
		redisUtil.set(autosaveKey, autosaveData, 24, TimeUnit.HOURS);
		log.debug("Autosaved content for identifier: {}", request.identifier());
			return ApiResponse.success("Content autosaved.", null);
	}

	@Override
	public ApiResponse<PostAutosaveResponse> retrieveAutosavedContent(String identifier)
	{
		String autosaveKey = autosaveKey(identifier);
		return redisUtil.get(autosaveKey, PostAutosaveResponse.class).map(ApiResponse::success).orElse(
			ApiResponse.error(BusinessCode.NOT_FOUND.getCode(), "No autosaved content was found for this identifier"));
	}

	// --- Private Helper Methods ---

	private Long mergeRealtimeViews(Long postId, Long dbViews)
	{
		Long totalViews = dbViews != null ? dbViews : 0L;
		Object extraViews = redisUtil.hashGet(CacheConstants.POST_VIEW_EXTRA_HASH, postId.toString());
		if (extraViews instanceof Number extraViewCount)
		{
			totalViews += extraViewCount.longValue();
		}
		return totalViews;
	}

	private List<PostDigestResponse> selectDistinctDigests(List<Post> candidates, Set<Long> selectedPostIds,
			int limit)
	{
		return candidates.stream().filter(post -> post.getId() != null && selectedPostIds.add(post.getId()))
				.limit(limit).map(postMapper::toDigestResponse).toList();
	}

	private void clearAutosaveData(String identifier)
	{
		if (identifier != null)
		{
			redisUtil.delete(autosaveKey(identifier));
		}
	}

	private String autosaveKey(String identifier)
	{
		String username = SecurityUtil.getCurrentUsername();
		Assert.notBlank(username, () -> new BusinessException(BusinessCode.UNAUTHORIZED, "Authentication required"));
		return CacheConstants.POST_AUTOSAVE_PREFIX + username + ":" + identifier;
	}

	private Post findPostOrThrow(Long id)
	{
		return postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
	}

	private Post findPostForUpdateOrThrow(Long id)
	{
		return postRepository.findByIdForUpdate(id)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
	}

	private String validateAndGenerateSlug(String requestedSlug, String title)
	{
		String slug = StrUtil.isBlank(requestedSlug) ? SlugUtil.toSlug(title) : SlugUtil.toSlug(requestedSlug);
		Assert.isFalse(postRepository.findBySlug(slug).isPresent(),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Post slug is already in use: " + slug));
		return slug;
	}

	private void syncParentPost(Post post, PostRequest request)
	{
		if (request.parentId() != null)
		{
			if (post.getId() != null && request.parentId().equals(post.getId()))
			{
				throw new BusinessException(BusinessCode.BAD_REQUEST, "A post cannot be its own parent");
			}

			Post parent = findPostOrThrow(request.parentId());
			if (post.getId() != null && parent.getPath() != null
					&& parent.getPath().contains("/" + post.getId() + "/"))
			{
				throw new BusinessException(BusinessCode.BAD_REQUEST,
						"A post cannot be moved below one of its descendants");
			}
			post.setParent(parent);
			post.updatePath(parent);
		}
		else
		{
			post.setParent(null);
			post.updatePath(null);
		}
	}

	private void syncCategoryAndTags(Post post, PostRequest request)
	{
		// Sync Category
		if (request.categoryId() != null)
		{
			post.setCategory(categoryRepository.findById(request.categoryId())
					.orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.categoryId())));
		}
		else
		{
			post.setCategory(null);
		}

		// Sync Series
		if (request.seriesId() != null)
		{
			post.setSeries(seriesRepository.findById(request.seriesId())
					.orElseThrow(() -> new ResourceNotFoundException("Series", "id", request.seriesId())));
			post.setSeriesOrder(request.seriesOrder() != null ? request.seriesOrder() : 0);
		}
		else
		{
			post.setSeries(null);
			post.setSeriesOrder(0);
		}

		// Sync Tags - Fix: handle empty/null tagIds correctly to clear existing tags
		if (request.tagIds() != null)
		{
			if (request.tagIds().isEmpty())
			{
				post.setTags(new HashSet<>());
			}
			else
			{
				List<space.nebula.nexus.entity.Tag> tags = tagRepository.findAllById(request.tagIds());
				Assert.isTrue(tags.size() == request.tagIds().size(),
						() -> new BusinessException(BusinessCode.BAD_REQUEST, "One or more selected tags do not exist"));
				post.setTags(new HashSet<>(tags));
			}
		}
	}
}
