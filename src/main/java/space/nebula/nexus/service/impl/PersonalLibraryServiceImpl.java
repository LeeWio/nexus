package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.PostCollection;
import space.nebula.nexus.entity.PostCollectionItem;
import space.nebula.nexus.entity.ReadingHistory;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.mapper.CategoryMapper;
import space.nebula.nexus.payload.request.PostCollectionRequest;
import space.nebula.nexus.payload.request.ReadingProgressRequest;
import space.nebula.nexus.payload.response.CollectionPostResponse;
import space.nebula.nexus.payload.response.ContentPreferenceResponse;
import space.nebula.nexus.payload.response.FavoritePostResponse;
import space.nebula.nexus.payload.response.LikedPostResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PersonalLibraryOverviewResponse;
import space.nebula.nexus.payload.response.PostCollectionResponse;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.ReadingHistoryResponse;
import space.nebula.nexus.payload.response.RecommendedPostResponse;
import space.nebula.nexus.repository.PostCollectionItemRepository;
import space.nebula.nexus.repository.CategoryFollowRepository;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.HiddenRecommendationRepository;
import space.nebula.nexus.repository.PostCollectionRepository;
import space.nebula.nexus.repository.PostFavoriteRepository;
import space.nebula.nexus.repository.PostLikeRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.ReadingHistoryRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.util.SecurityUtil;
import space.nebula.nexus.service.IPersonalLibraryService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Transactional implementation of the authenticated user's personal content
 * library.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalLibraryServiceImpl implements IPersonalLibraryService {
	private static final long MAX_COLLECTIONS_PER_USER = 50;
	private static final long MAX_POSTS_PER_COLLECTION = 1000;
	private static final int OVERVIEW_SECTION_SIZE = 6;
	private static final int OVERVIEW_CANDIDATE_SIZE = 18;
	private static final int PREFERENCE_CATEGORY_SIZE = 5;
	private static final long MAX_FOLLOWED_CATEGORIES = 20;
	private static final String FOLLOWED_CATEGORY_REASON = "FOLLOWED_CATEGORY";
	private static final String CATEGORY_INTEREST_REASON = "CATEGORY_INTEREST";
	private static final String COMMUNITY_POPULAR_REASON = "COMMUNITY_POPULAR";

	private final UserRepository userRepository;
	private final CategoryRepository categoryRepository;
	private final PostRepository postRepository;
	private final CategoryFollowRepository categoryFollowRepository;
	private final HiddenRecommendationRepository hiddenRecommendationRepository;
	private final PostFavoriteRepository favoriteRepository;
	private final PostLikeRepository likeRepository;
	private final ReadingHistoryRepository readingHistoryRepository;
	private final PostCollectionRepository collectionRepository;
	private final PostCollectionItemRepository collectionItemRepository;
	private final PostMapper postMapper;
	private final CategoryMapper categoryMapper;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PersonalLibraryOverviewResponse> getOverview() {
		User user = currentUser();
		Pageable sectionCandidates = PageRequest.of(0, OVERVIEW_CANDIDATE_SIZE);

		List<ReadingHistoryResponse> continueReading = readingHistoryRepository
				.findContinuableHistory(user.getId(), PostStatus.PUBLISHED, sectionCandidates).stream()
				.limit(OVERVIEW_SECTION_SIZE).map(this::toReadingHistoryResponse).toList();
		Set<Long> selectedPostIds = continueReading.stream().map(entry -> entry.post().id())
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

		List<FavoritePostResponse> recentFavorites = favoriteRepository
				.findVisibleFavorites(user.getId(), PostStatus.PUBLISHED, sectionCandidates).stream()
				.filter(favorite -> selectedPostIds.add(favorite.getPost().getId())).limit(OVERVIEW_SECTION_SIZE)
				.map(favorite -> new FavoritePostResponse(postMapper.toDigestResponse(favorite.getPost()),
						favorite.getCreatedAt()))
				.toList();

		List<Long> followedCategoryIds = categoryFollowRepository.findCategoryIdsByUserId(user.getId());
		List<Long> preferredCategoryIds = preferredCategoryIds(user.getId(), followedCategoryIds);
		List<RecommendedPostResponse> recommendations = buildRecommendations(user.getId(), preferredCategoryIds,
				Set.copyOf(followedCategoryIds));
		return ApiResponse
				.success(new PersonalLibraryOverviewResponse(continueReading, recentFavorites, recommendations));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<PostDigestResponse>> getFollowingFeed(Pageable pageable) {
		User user = currentUser();
		var feed = postRepository.findFollowedCategoryFeed(user.getId(), PostStatus.PUBLISHED, pageable)
				.map(postMapper::toDigestResponse);
		return ApiResponse.success(PageResult.of(feed));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<ContentPreferenceResponse> getContentPreferences() {
		User user = currentUser();
		var categories = categoryFollowRepository.findAllByUserId(user.getId()).stream()
				.map(follow -> follow.getCategory()).toList();
		long hiddenPostCount = hiddenRecommendationRepository.countByUserIdAndIsDeletedFalse(user.getId());
		return ApiResponse
				.success(new ContentPreferenceResponse(categoryMapper.toResponseList(categories), hiddenPostCount));
	}

	@Override
	@Transactional
	public ApiResponse<Void> followCategory(Long categoryId) {
		User user = currentUser();
		categoryRepository.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
		if (categoryFollowRepository.existsByUserIdAndCategoryIdAndIsDeletedFalse(user.getId(), categoryId)) {
			return ApiResponse.success("Category is already followed", null);
		}
		Assert.isTrue(categoryFollowRepository.countByUserIdAndIsDeletedFalse(user.getId()) < MAX_FOLLOWED_CATEGORIES,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "You can follow up to 20 categories"));
		categoryFollowRepository.insertIgnore(user.getId(), categoryId);
		return ApiResponse.success("Category followed", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> unfollowCategory(Long categoryId) {
		User user = currentUser();
		int deleted = categoryFollowRepository.deleteOwnedFollow(user.getId(), categoryId);
		return ApiResponse.success(deleted > 0 ? "Category unfollowed" : "Category was not followed", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> hideRecommendation(Long postId) {
		User user = currentUser();
		findPublishedPost(postId);
		int inserted = hiddenRecommendationRepository.insertIgnore(user.getId(), postId);
		return ApiResponse.success(inserted > 0 ? "Recommendation hidden" : "Recommendation is already hidden", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> restoreRecommendation(Long postId) {
		User user = currentUser();
		int deleted = hiddenRecommendationRepository.deleteOwnedHiddenPost(user.getId(), postId);
		return ApiResponse.success(deleted > 0 ? "Recommendation restored" : "Recommendation was not hidden", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> clearHiddenRecommendations() {
		User user = currentUser();
		int deleted = hiddenRecommendationRepository.deleteAllOwnedHiddenPosts(user.getId());
		log.info("Cleared {} hidden recommendations for user {}", deleted, user.getUsername());
		return ApiResponse.success("Hidden recommendations cleared", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<FavoritePostResponse>> getFavorites(Pageable pageable) {
		User user = currentUser();
		var favorites = favoriteRepository.findVisibleFavorites(user.getId(), PostStatus.PUBLISHED, pageable)
				.map(favorite -> new FavoritePostResponse(postMapper.toDigestResponse(favorite.getPost()),
						favorite.getCreatedAt()));
		return ApiResponse.success(PageResult.of(favorites));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<LikedPostResponse>> getLikedPosts(Pageable pageable) {
		User user = currentUser();
		var likedPosts = likeRepository.findVisibleLikes(user.getId(), PostStatus.PUBLISHED, pageable)
				.map(postLike -> new LikedPostResponse(postMapper.toDigestResponse(postLike.getPost()),
						postLike.getCreatedAt()));
		return ApiResponse.success(PageResult.of(likedPosts));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<ReadingHistoryResponse>> getReadingHistory(Pageable pageable) {
		User user = currentUser();
		var history = readingHistoryRepository.findVisibleHistory(user.getId(), PostStatus.PUBLISHED, pageable)
				.map(this::toReadingHistoryResponse);
		return ApiResponse.success(PageResult.of(history));
	}

	@Override
	@Transactional
	public ApiResponse<ReadingHistoryResponse> recordReadingProgress(Long postId, ReadingProgressRequest request) {
		User user = currentUser();
		Post post = findPublishedPost(postId);
		ReadingHistory history = readingHistoryRepository.findByUserIdAndPostIdAndIsDeletedFalse(user.getId(), postId)
				.orElseGet(() -> newReadingHistory(user, post));
		history.recordProgress(request.progressPercent(), normalizeOptional(request.positionAnchor()));
		ReadingHistory saved = readingHistoryRepository.save(history);
		return ApiResponse.success("Reading progress saved", toReadingHistoryResponse(saved));
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteReadingHistory(Long postId) {
		User user = currentUser();
		int deleted = readingHistoryRepository.deleteOwnedEntry(user.getId(), postId);
		Assert.isTrue(deleted > 0, () -> new ResourceNotFoundException("ReadingHistory", "postId", postId));
		return ApiResponse.success("Reading history entry removed", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> clearReadingHistory() {
		User user = currentUser();
		int deleted = readingHistoryRepository.deleteAllOwnedEntries(user.getId());
		log.info("Cleared {} reading history entries for user {}", deleted, user.getUsername());
		return ApiResponse.success("Reading history cleared", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<PostCollectionResponse>> getCollections() {
		return ApiResponse.success(collectionRepository.findSummariesByUserId(currentUser().getId()));
	}

	@Override
	@Transactional
	public ApiResponse<PostCollectionResponse> createCollection(PostCollectionRequest request) {
		User user = currentUser();
		String name = normalizeName(request.name());
		Assert.isTrue(collectionRepository.countByUserIdAndIsDeletedFalse(user.getId()) < MAX_COLLECTIONS_PER_USER,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "You can create up to 50 collections"));
		Assert.isFalse(collectionRepository.existsByUserIdAndNameIgnoreCaseAndIsDeletedFalse(user.getId(), name),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "A collection with this name already exists"));

		PostCollection collection = new PostCollection();
		collection.setUser(user);
		applyCollectionRequest(collection, name, request.description());
		PostCollection saved = collectionRepository.save(collection);
		return ApiResponse.success("Collection created", toCollectionResponse(saved, 0L));
	}

	@Override
	@Transactional
	public ApiResponse<PostCollectionResponse> updateCollection(Long collectionId, PostCollectionRequest request) {
		User user = currentUser();
		PostCollection collection = findOwnedCollection(collectionId, user.getId());
		String name = normalizeName(request.name());
		Assert.isFalse(
				collectionRepository.existsByUserIdAndNameIgnoreCaseAndIdNotAndIsDeletedFalse(user.getId(), name,
						collectionId),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "A collection with this name already exists"));
		applyCollectionRequest(collection, name, request.description());
		PostCollection saved = collectionRepository.save(collection);
		long itemCount = collectionItemRepository.countByCollectionIdAndIsDeletedFalse(collectionId);
		return ApiResponse.success("Collection updated", toCollectionResponse(saved, itemCount));
	}

	@Override
	@Transactional
	public ApiResponse<Void> deleteCollection(Long collectionId) {
		User user = currentUser();
		PostCollection collection = findOwnedCollection(collectionId, user.getId());
		collectionItemRepository.deleteAllItems(collectionId);
		collectionRepository.delete(collection);
		return ApiResponse.success("Collection deleted", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<CollectionPostResponse>> getCollectionPosts(Long collectionId, Pageable pageable) {
		User user = currentUser();
		findOwnedCollection(collectionId, user.getId());
		var items = collectionItemRepository.findVisibleItems(collectionId, PostStatus.PUBLISHED, pageable).map(
				item -> new CollectionPostResponse(postMapper.toDigestResponse(item.getPost()), item.getCreatedAt()));
		return ApiResponse.success(PageResult.of(items));
	}

	@Override
	@Transactional
	public ApiResponse<Void> addPostToCollection(Long collectionId, Long postId) {
		User user = currentUser();
		PostCollection collection = findOwnedCollection(collectionId, user.getId());
		Post post = findPublishedPost(postId);
		if (collectionItemRepository.existsByCollectionIdAndPostIdAndIsDeletedFalse(collectionId, postId)) {
			return ApiResponse.success("Post is already in this collection", null);
		}
		Assert.isTrue(
				collectionItemRepository.countByCollectionIdAndIsDeletedFalse(collectionId) < MAX_POSTS_PER_COLLECTION,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "A collection can contain up to 1,000 posts"));
		PostCollectionItem item = new PostCollectionItem();
		item.setCollection(collection);
		item.setPost(post);
		collectionItemRepository.save(item);
		return ApiResponse.success("Post added to collection", null);
	}

	@Override
	@Transactional
	public ApiResponse<Void> removePostFromCollection(Long collectionId, Long postId) {
		User user = currentUser();
		findOwnedCollection(collectionId, user.getId());
		int deleted = collectionItemRepository.deleteItem(collectionId, postId);
		Assert.isTrue(deleted > 0, () -> new ResourceNotFoundException("CollectionPost", "postId", postId));
		return ApiResponse.success("Post removed from collection", null);
	}

	private User currentUser() {
		return SecurityUtil.getCurrentUserOrThrow(userRepository);
	}

	private Post findPublishedPost(Long postId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
		Assert.isTrue(post.isPublished(), () -> new BusinessException(BusinessCode.POST_NOT_PUBLISHED));
		return post;
	}

	private PostCollection findOwnedCollection(Long collectionId, Long userId) {
		return collectionRepository.findByIdAndUserIdAndIsDeletedFalse(collectionId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("PostCollection", "id", collectionId));
	}

	private ReadingHistory newReadingHistory(User user, Post post) {
		ReadingHistory history = new ReadingHistory();
		history.setUser(user);
		history.setPost(post);
		return history;
	}

	private ReadingHistoryResponse toReadingHistoryResponse(ReadingHistory history) {
		return new ReadingHistoryResponse(postMapper.toDigestResponse(history.getPost()), history.getProgressPercent(),
				history.getPositionAnchor(), history.getLastReadAt(), history.getCompletedAt());
	}

	private List<Long> preferredCategoryIds(Long userId, List<Long> followedCategoryIds) {
		Pageable preferenceLimit = PageRequest.of(0, PREFERENCE_CATEGORY_SIZE);
		Set<Long> categoryIds = new LinkedHashSet<>();
		categoryIds.addAll(followedCategoryIds);
		categoryIds.addAll(favoriteRepository.findPreferredCategoryIds(userId, PostStatus.PUBLISHED, preferenceLimit));
		categoryIds.addAll(likeRepository.findPreferredCategoryIds(userId, PostStatus.PUBLISHED, preferenceLimit));
		categoryIds.addAll(
				readingHistoryRepository.findPreferredCategoryIds(userId, PostStatus.PUBLISHED, preferenceLimit));
		return categoryIds.stream().limit(PREFERENCE_CATEGORY_SIZE).toList();
	}

	private List<RecommendedPostResponse> buildRecommendations(Long userId, List<Long> preferredCategoryIds,
			Set<Long> followedCategoryIds) {
		Pageable candidateLimit = PageRequest.of(0, OVERVIEW_CANDIDATE_SIZE);
		List<RecommendedPostResponse> recommendations = new ArrayList<>(OVERVIEW_SECTION_SIZE);
		Set<Long> selectedIds = new LinkedHashSet<>();

		if (!preferredCategoryIds.isEmpty()) {
			postRepository
					.findPersonalizedRecommendations(userId, preferredCategoryIds, PostStatus.PUBLISHED, candidateLimit)
					.forEach(post -> addRecommendation(recommendations, selectedIds, post, followedCategoryIds, true));
		}
		if (recommendations.size() < OVERVIEW_SECTION_SIZE) {
			postRepository.findPopularUnseenPosts(userId, PostStatus.PUBLISHED, candidateLimit)
					.forEach(post -> addRecommendation(recommendations, selectedIds, post, followedCategoryIds, false));
		}
		return List.copyOf(recommendations);
	}

	private void addRecommendation(List<RecommendedPostResponse> recommendations, Set<Long> selectedIds, Post post,
			Set<Long> followedCategoryIds, boolean categoryBased) {
		if (recommendations.size() >= OVERVIEW_SECTION_SIZE || post.getId() == null || !selectedIds.add(post.getId())) {
			return;
		}
		if (categoryBased && post.getCategory() != null) {
			if (followedCategoryIds.contains(post.getCategory().getId())) {
				recommendations.add(new RecommendedPostResponse(postMapper.toDigestResponse(post),
						FOLLOWED_CATEGORY_REASON, "Because you follow " + post.getCategory().getName() + "."));
				return;
			}
			recommendations.add(new RecommendedPostResponse(postMapper.toDigestResponse(post), CATEGORY_INTEREST_REASON,
					"Recommended because you often read " + post.getCategory().getName() + "."));
			return;
		}
		recommendations.add(new RecommendedPostResponse(postMapper.toDigestResponse(post), COMMUNITY_POPULAR_REASON,
				"Popular across the community."));
	}

	private PostCollectionResponse toCollectionResponse(PostCollection collection, Long itemCount) {
		return new PostCollectionResponse(collection.getId(), collection.getName(), collection.getDescription(),
				itemCount, collection.getCreatedAt(), collection.getUpdatedAt());
	}

	private void applyCollectionRequest(PostCollection collection, String name, String description) {
		collection.setName(name);
		collection.setDescription(normalizeOptional(description));
	}

	private String normalizeName(String name) {
		return name.trim().replaceAll("\\s+", " ");
	}

	private String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
