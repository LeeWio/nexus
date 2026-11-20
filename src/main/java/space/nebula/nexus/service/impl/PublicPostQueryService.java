package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.config.BlogDiscoveryProperties;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;
import space.nebula.nexus.payload.response.BlogFacetResponse;
import space.nebula.nexus.payload.response.CategoryResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.SeriesSummaryResponse;
import space.nebula.nexus.repository.ConfigRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PostSeriesRepository;
import space.nebula.nexus.repository.specification.PostSpecification;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.service.IPublicPostQueryService;
import space.nebula.nexus.service.PostRankingService;
import space.nebula.nexus.service.support.PostCommentCountSupport;
import space.nebula.nexus.utils.PostContentAnalyzer;
import space.nebula.nexus.utils.RedisUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicPostQueryService implements IPublicPostQueryService {

	private final PostRepository postRepository;
	private final PostSeriesRepository seriesRepository;
	private final PostMapper postMapper;
	private final RedisUtil redisUtil;
	private final IInteractionService interactionService;
	private final PostRankingService postRankingService;
	private final BlogDiscoveryProperties discoveryProperties;
	private final ObjectProvider<MeterRegistry> meterRegistryProvider;
	private final PostCommentCountSupport postCommentCountSupport;
	private final ConfigRepository configRepository;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_POSTS, key = CacheConstants.POST_LIST_KEY, sync = true)
	public ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Pageable pageable) {
		return searchPublicPosts(categoryId, tagId, null, keyword, null, null, null, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable) {
		return searchPublicPosts(categoryId, tagId, null, keyword, featuredOnly, hasCover, contentType, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String authorName,
			String keyword, Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable) {
		var spec = PostSpecification.filterPublicPosts(categoryId, tagId, authorName, keyword, featuredOnly, hasCover,
				contentType);
		Page<Post> publishedPosts = postRepository.findAll(spec, pageable);
		return ApiResponse.success(PageResult.of(publishedPosts.map(postMapper::toResponse)));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostDigestResponse>> searchPublicPostDigests(Long categoryId, Long tagId,
			String keyword, Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable) {
		return searchPublicPostDigests(categoryId, tagId, null, keyword, featuredOnly, hasCover, contentType, pageable);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostDigestResponse>> searchPublicPostDigests(Long categoryId, Long tagId,
			String authorName, String keyword, Boolean featuredOnly, Boolean hasCover, PostContentType contentType,
			Pageable pageable) {
		var spec = PostSpecification.filterPublicPosts(categoryId, tagId, authorName, keyword, featuredOnly, hasCover,
				contentType);
		Page<Post> publishedPosts = postRepository.findAll(spec, pageable);
		postCommentCountSupport.attachApprovedRootCounts(publishedPosts.getContent());
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
		Page<Post> archivePosts = postRepository.findAll(archiveSpec(year, month), pageable);
		postCommentCountSupport.attachApprovedRootCounts(archivePosts.getContent());
		Page<PostDigestResponse> archivePage = archivePosts.map(postMapper::toDigestResponse);
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
		Page<Post> featuredPosts = postRepository.findProminentPublicPosts(PostStatus.PUBLISHED, pageable);
		postCommentCountSupport.attachApprovedRootCounts(featuredPosts.getContent());
		Page<PostDigestResponse> page = featuredPosts.map(postMapper::toDigestResponse);
		return ApiResponse.success(PageResult.of(page));
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_DISCOVERY, key = CacheConstants.BLOG_DISCOVERY_KEY, sync = true)
	public ApiResponse<BlogDiscoveryResponse> retrievePublicDiscovery() {
		List<Post> discoveryCandidates = postRepository.findDiscoveryCandidates(PostStatus.PUBLISHED,
				PageRequest.of(0, candidateSize()));
		MeterRegistry meterRegistry = meterRegistryProvider == null ? null : meterRegistryProvider.getIfAvailable();
		if (meterRegistry != null) {
			meterRegistry.counter("nexus.blog.discovery.snapshots").increment();
			meterRegistry.counter("nexus.blog.discovery.candidates").increment(discoveryCandidates.size());
			if (discoveryCandidates.isEmpty()) {
				meterRegistry.counter("nexus.blog.discovery.empty").increment();
			}
		}
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
		List<PostDigestResponse> trending = selectDistinctDigests(
				scoredCandidates.stream()
						.sorted(Comparator.comparingDouble(postRankingService::trendingScore).reversed()
								.thenComparing(Post::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
								.thenComparing(Post::getId, Comparator.nullsLast(Comparator.reverseOrder())))
						.toList(),
				selectedPostIds, sectionSize());
		Sort mostReadFirst = Sort.by(Sort.Order.desc("views"), Sort.Order.desc("likesCount"),
				Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));
		List<Post> mostReadCandidates = postRepository
				.findAllByStatus(PostStatus.PUBLISHED, PageRequest.of(0, candidateSize(), mostReadFirst)).getContent();
		List<PostDigestResponse> mostRead = selectDistinctDigests(mostReadCandidates, selectedPostIds, sectionSize());

		if (spotlight != null) {
			postCommentCountSupport.attachApprovedRootCounts(List.of(spotlight));
		}
		PostDigestResponse spotlightResponse = spotlight == null ? null : postMapper.toDigestResponse(spotlight);
		return ApiResponse.success(new BlogDiscoveryResponse(spotlightResponse, curated, latest, trending, mostRead,
				buildPublicSeriesSummaries(), buildCategoryGroups(scoredCandidates, selectedPostIds), Instant.now(),
				discoveryProperties.getAlgorithmVersion()));
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
			redisUtil.set(cacheKey, mappedResponse, 1, TimeUnit.HOURS);
			return mappedResponse;
		});
		redisUtil.hashIncrement(CacheConstants.POST_VIEW_EXTRA_HASH, postResponse.id().toString(), 1L);
		PostResponse.PostResponseBuilder responseBuilder = postResponse.toBuilder();
		responseBuilder.views(mergeRealtimeViews(postResponse.id(), postResponse.views()));
		interactionService.populateInteractionData(responseBuilder, postResponse.id());
		populateWikiMetadata(responseBuilder, postResponse);
		return ApiResponse.success(responseBuilder.build());
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.BLOG_RELATED, key = "'related:v3:' + #slug + ':' + #pageable", sync = true)
	public ApiResponse<List<PostDigestResponse>> retrieveRelatedPosts(String slug, Pageable pageable) {
		Post source = postRepository.findBySlug(slug)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));
		Assert.isTrue(source.isPublished(),
				() -> new BusinessException(BusinessCode.FORBIDDEN, "Post is not published"));

		int requestedSize = pageable.isPaged() ? pageable.getPageSize() : sectionSize();
		long offset = pageable.isPaged() ? pageable.getOffset() : 0;
		int candidateLimit = Math.max(120, candidateSize());
		if (offset >= candidateLimit) {
			return ApiResponse.success(List.of());
		}
		Pageable candidatePage = PageRequest.of(0, candidateLimit,
				Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
		record RankedPost(Post post, double score) {
		}
		List<Post> relatedCandidates = postRepository.findAll(relatedPostsSpec(source), candidatePage).getContent()
				.stream()
				.map(candidate -> new RankedPost(candidate, postRankingService.relatedScore(source, candidate)))
				.sorted(Comparator.comparingDouble(RankedPost::score).reversed()
						.thenComparing(ranked -> ranked.post().getPublishedAt(),
								Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(ranked -> ranked.post().getId(),
								Comparator.nullsLast(Comparator.reverseOrder())))
				.skip(offset).limit(requestedSize).map(RankedPost::post).toList();
		postCommentCountSupport.attachApprovedRootCounts(relatedCandidates);
		List<PostDigestResponse> relatedPosts = relatedCandidates.stream().map(postMapper::toDigestResponse).toList();
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

	private void populateWikiMetadata(PostResponse.PostResponseBuilder builder, PostResponse post) {
		if (StrUtil.isNotBlank(post.path())) {
			String[] parts = post.path().split("/");
			List<String> ancestorPaths = new ArrayList<>();
			StringBuilder pathBuilder = new StringBuilder();
			for (String part : parts) {
				if (StrUtil.isNotBlank(part)) {
					pathBuilder.append("/").append(part).append("/");
					ancestorPaths.add(pathBuilder.toString());
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
		if (post.series() != null) {
			var prev = postRepository.findPreviousInSeries(post.series().id(), post.seriesOrder());
			var next = postRepository.findNextInSeries(post.series().id(), post.seriesOrder());
			builder.navigation(new PostResponse.Navigation(
					prev.map(p -> new PostResponse.Navigation.Neighbor(p.getTitle(), p.getSlug())).orElse(null),
					next.map(p -> new PostResponse.Navigation.Neighbor(p.getTitle(), p.getSlug())).orElse(null)));
		}
		String siteUrl = configRepository.findByConfigKey("site_url")
				.map(space.nebula.nexus.entity.Config::getConfigValue).orElse("http://localhost:3000");
		String fullUrl = siteUrl + "/post/" + post.slug();
		builder.seo(
				new PostResponse.SeoMetadata(post.title(), StrUtil.blankToDefault(post.summary(), post.autoSummary()),
						post.coverImage(), "article", fullUrl, "summary_large_image", fullUrl));
	}

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
		List<Post> selected = candidates.stream()
				.filter(post -> post.getId() != null && selectedPostIds.add(post.getId())).limit(limit).toList();
		postCommentCountSupport.attachApprovedRootCounts(selected);
		return selected.stream().map(postMapper::toDigestResponse).toList();
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
			postCommentCountSupport.attachApprovedRootCounts(rankedPosts);
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

	private int seriesSize() {
		return positive(discoveryProperties.getSeriesSize(), 4);
	}

	private int candidateSize() {
		return positive(discoveryProperties.getCandidateSize(), 48);
	}

	private List<SeriesSummaryResponse> buildPublicSeriesSummaries() {
		return java.util.Optional.ofNullable(seriesRepository.findPublicColumnSummaries(PostStatus.PUBLISHED))
				.orElseGet(List::of).stream().limit(seriesSize())
				.map(series -> new SeriesSummaryResponse(series.getId(), series.getName(), series.getSlug(),
						series.getDescription(), series.getCoverImage(), Math.toIntExact(series.getPostsCount()),
						series.getCreatedAt()))
				.toList();
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

	private Post findPostOrThrow(Long id) {
		return postRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
	}

	private String previewContentHash(Post post) {
		if (StrUtil.isNotBlank(post.getContentHash())) {
			return post.getContentHash();
		}
		return PostContentAnalyzer.analyze(post.getTitle(), post.getSummary(), post.getContent(), post.getContentType())
				.contentHash();
	}
}
