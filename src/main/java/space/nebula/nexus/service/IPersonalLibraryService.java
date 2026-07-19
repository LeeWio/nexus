package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PostCollectionRequest;
import space.nebula.nexus.payload.request.ReadingProgressRequest;
import space.nebula.nexus.payload.response.CollectionPostResponse;
import space.nebula.nexus.payload.response.ContentPreferenceResponse;
import space.nebula.nexus.payload.response.FavoritePostResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PersonalLibraryOverviewResponse;
import space.nebula.nexus.payload.response.PostCollectionResponse;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.ReadingHistoryResponse;

import java.util.List;

/**
 * Provides the authenticated user's favorites, reading history, and custom collections.
 */
public interface IPersonalLibraryService
{
	/**
	 * Returns the current user's aggregated personal content overview.
	 *
	 * @return continue-reading, recent-favorite, and recommendation sections
	 */
	ApiResponse<PersonalLibraryOverviewResponse> getOverview();

	/**
	 * Returns published posts from categories followed by the current user.
	 *
	 * @param pageable pagination settings
	 * @return followed-category content feed
	 */
	ApiResponse<PageResult<PostDigestResponse>> getFollowingFeed(Pageable pageable);

	/**
	 * Returns explicit content preferences owned by the current user.
	 *
	 * @return followed categories and hidden recommendation count
	 */
	ApiResponse<ContentPreferenceResponse> getContentPreferences();

	/**
	 * Follows a category for future recommendations.
	 *
	 * @param categoryId category identifier
	 * @return operation result
	 */
	ApiResponse<Void> followCategory(Long categoryId);

	/**
	 * Stops following a category.
	 *
	 * @param categoryId category identifier
	 * @return operation result
	 */
	ApiResponse<Void> unfollowCategory(Long categoryId);

	/**
	 * Hides a published post from future recommendations.
	 *
	 * @param postId post identifier
	 * @return operation result
	 */
	ApiResponse<Void> hideRecommendation(Long postId);

	/**
	 * Restores a post previously hidden from recommendations.
	 *
	 * @param postId post identifier
	 * @return operation result
	 */
	ApiResponse<Void> restoreRecommendation(Long postId);

	/**
	 * Clears all hidden recommendation feedback owned by the current user.
	 *
	 * @return operation result
	 */
	ApiResponse<Void> clearHiddenRecommendations();

	/** Returns the current user's visible favorite posts. */
	ApiResponse<PageResult<FavoritePostResponse>> getFavorites(Pageable pageable);

	/** Returns the current user's visible reading history. */
	ApiResponse<PageResult<ReadingHistoryResponse>> getReadingHistory(Pageable pageable);

	/** Records or replaces the current user's reading position for a published post. */
	ApiResponse<ReadingHistoryResponse> recordReadingProgress(Long postId, ReadingProgressRequest request);

	/** Removes one post from the current user's reading history. */
	ApiResponse<Void> deleteReadingHistory(Long postId);

	/** Clears the current user's complete reading history. */
	ApiResponse<Void> clearReadingHistory();

	/** Returns summaries of the current user's custom collections. */
	ApiResponse<List<PostCollectionResponse>> getCollections();

	/** Creates a custom collection. */
	ApiResponse<PostCollectionResponse> createCollection(PostCollectionRequest request);

	/** Updates a custom collection owned by the current user. */
	ApiResponse<PostCollectionResponse> updateCollection(Long collectionId, PostCollectionRequest request);

	/** Deletes a custom collection and its memberships. */
	ApiResponse<Void> deleteCollection(Long collectionId);

	/** Returns the visible posts stored in a custom collection. */
	ApiResponse<PageResult<CollectionPostResponse>> getCollectionPosts(Long collectionId, Pageable pageable);

	/** Adds a published post to a custom collection. */
	ApiResponse<Void> addPostToCollection(Long collectionId, Long postId);

	/** Removes a post from a custom collection. */
	ApiResponse<Void> removePostFromCollection(Long collectionId, Long postId);
}
