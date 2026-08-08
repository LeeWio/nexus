package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
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
import space.nebula.nexus.service.IPersonalLibraryService;

import java.util.List;

/**
 * Exposes the authenticated user's personal content library.
 */
@Tag(name = "Personal Library", description = "Favorites, reading history, and personal post collections")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/user/library")
@RequiredArgsConstructor
public class PersonalLibraryController {
	private final IPersonalLibraryService personalLibraryService;

	/**
	 * Returns the current user's personalized library landing content.
	 *
	 * @return aggregated personal content sections
	 */
	@GetMapping("/overview")
	@Operation(summary = "Get personal library overview", description = "Returns the authenticated user's continue-reading sessions, recent favorites, and recommendations. Recommendation reason fields are intended for UI explanations.")
	public ApiResponse<PersonalLibraryOverviewResponse> getOverview() {
		return personalLibraryService.getOverview();
	}

	/**
	 * Returns the current user's followed-category content feed.
	 *
	 * @param pageable
	 *            pagination settings
	 * @return followed-category post page
	 */
	@GetMapping("/following")
	@Operation(summary = "Get followed-category content feed", description = "Returns recently published posts from categories explicitly followed by the current user. An empty result means no followed category currently has matching published content.")
	public ApiResponse<PageResult<PostDigestResponse>> getFollowingFeed(
			@Parameter(description = "Zero-based request pagination. Responses use a one-based page number.") @PageableDefault(size = 20) Pageable pageable) {
		return personalLibraryService.getFollowingFeed(pageable);
	}

	/**
	 * Returns user-controlled content preferences.
	 *
	 * @return followed categories and hidden recommendation count
	 */
	@GetMapping("/preferences")
	@Operation(summary = "Get content preferences", description = "Returns categories the current user explicitly follows and the count of posts hidden from recommendations.")
	public ApiResponse<ContentPreferenceResponse> getContentPreferences() {
		return personalLibraryService.getContentPreferences();
	}

	/**
	 * Follows a category for future recommendations.
	 *
	 * @param categoryId
	 *            category identifier
	 * @return operation result
	 */
	@PutMapping("/preferences/categories/{categoryId}")
	@Operation(summary = "Follow a recommendation category", description = "Add a category to the current user's recommendation and followed-content preferences. Repeating the request is safe.")
	public ApiResponse<Void> followCategory(@Parameter(description = "Category ID") @PathVariable Long categoryId) {
		return personalLibraryService.followCategory(categoryId);
	}

	/**
	 * Stops following a recommendation category.
	 *
	 * @param categoryId
	 *            category identifier
	 * @return operation result
	 */
	@DeleteMapping("/preferences/categories/{categoryId}")
	@Operation(summary = "Unfollow a recommendation category", description = "Remove a category from the current user's recommendation and followed-content preferences. Repeating the request is safe.")
	public ApiResponse<Void> unfollowCategory(@Parameter(description = "Category ID") @PathVariable Long categoryId) {
		return personalLibraryService.unfollowCategory(categoryId);
	}

	/**
	 * Hides a post from future recommendations.
	 *
	 * @param postId
	 *            post identifier
	 * @return operation result
	 */
	@PutMapping("/preferences/hidden-posts/{postId}")
	@Operation(summary = "Hide a post from recommendations", description = "Hide one published post from future recommendation results for the current user. It does not affect favorites, likes, or normal public browsing.")
	public ApiResponse<Void> hideRecommendation(@Parameter(description = "Published post ID") @PathVariable Long postId) {
		return personalLibraryService.hideRecommendation(postId);
	}

	/**
	 * Restores one hidden recommendation.
	 *
	 * @param postId
	 *            post identifier
	 * @return operation result
	 */
	@DeleteMapping("/preferences/hidden-posts/{postId}")
	@Operation(summary = "Restore a hidden recommendation", description = "Allow one previously hidden post to appear in future recommendations again.")
	public ApiResponse<Void> restoreRecommendation(@Parameter(description = "Published post ID") @PathVariable Long postId) {
		return personalLibraryService.restoreRecommendation(postId);
	}

	/**
	 * Clears all hidden recommendation feedback.
	 *
	 * @return operation result
	 */
	@DeleteMapping("/preferences/hidden-posts")
	@Operation(summary = "Clear hidden recommendation feedback", description = "Remove all hidden-post feedback for the current user so every eligible post can be recommended again.")
	public ApiResponse<Void> clearHiddenRecommendations() {
		return personalLibraryService.clearHiddenRecommendations();
	}

	/** Returns the current user's favorite posts. */
	@GetMapping("/favorites")
	@Operation(summary = "Get favorite posts", description = "Return published posts currently bookmarked by the authenticated user.")
	public ApiResponse<PageResult<FavoritePostResponse>> getFavorites(
			@Parameter(description = "Zero-based request pagination. Responses use a one-based page number.") @PageableDefault(size = 20) Pageable pageable) {
		return personalLibraryService.getFavorites(pageable);
	}

	/** Returns the current user's liked posts. */
	@GetMapping("/likes")
	@Operation(summary = "Get liked posts", description = "Returns published articles liked by the current user, ordered by the most recent like. Unliked or unpublished posts are excluded.")
	public ApiResponse<PageResult<LikedPostResponse>> getLikedPosts(
			@Parameter(description = "Zero-based request pagination. Responses use a one-based page number.") @PageableDefault(size = 20) Pageable pageable) {
		return personalLibraryService.getLikedPosts(pageable);
	}

	/** Returns resumable reading history. */
	@GetMapping("/history")
	@Operation(summary = "Get reading history", description = "Return resumable reading sessions for the current user, including the latest stored percentage and frontend position anchor.")
	public ApiResponse<PageResult<ReadingHistoryResponse>> getReadingHistory(
			@PageableDefault(size = 20) Pageable pageable) {
		return personalLibraryService.getReadingHistory(pageable);
	}

	/** Records reading progress for a published post. */
	@PutMapping("/posts/{postId}/progress")
	@Operation(summary = "Save reading progress", description = "Create or replace the current user's progress for a published post. positionAnchor is frontend-defined and should remain stable across renders.")
	public ApiResponse<ReadingHistoryResponse> recordReadingProgress(
			@Parameter(description = "Published post ID") @PathVariable Long postId,
			@Valid @RequestBody ReadingProgressRequest request) {
		return personalLibraryService.recordReadingProgress(postId, request);
	}

	/** Removes one post from reading history. */
	@DeleteMapping("/history/{postId}")
	@Operation(summary = "Remove reading history entry", description = "Delete the current user's saved progress for one post without affecting favorites, likes, or recommendations.")
	public ApiResponse<Void> deleteReadingHistory(@Parameter(description = "Published post ID") @PathVariable Long postId) {
		return personalLibraryService.deleteReadingHistory(postId);
	}

	/** Clears all reading history owned by the current user. */
	@DeleteMapping("/history")
	@Operation(summary = "Clear reading history", description = "Delete every saved reading-progress entry owned by the current user.")
	public ApiResponse<Void> clearReadingHistory() {
		return personalLibraryService.clearReadingHistory();
	}

	/** Returns personal collection summaries. */
	@GetMapping("/collections")
	@Operation(summary = "Get personal collections", description = "Return all custom post collections owned by the current user, including their post counts.")
	public ApiResponse<List<PostCollectionResponse>> getCollections() {
		return personalLibraryService.getCollections();
	}

	/** Creates a personal collection. */
	@PostMapping("/collections")
	@Operation(summary = "Create personal collection", description = "Create a named collection owned by the current user. Collection names are unique per owner.")
	public ApiResponse<PostCollectionResponse> createCollection(@Valid @RequestBody PostCollectionRequest request) {
		return personalLibraryService.createCollection(request);
	}

	/** Updates a personal collection owned by the current user. */
	@PutMapping("/collections/{collectionId}")
	@Operation(summary = "Update personal collection", description = "Update the name or description of a collection owned by the current user.")
	public ApiResponse<PostCollectionResponse> updateCollection(
			@Parameter(description = "Collection ID") @PathVariable Long collectionId,
			@Valid @RequestBody PostCollectionRequest request) {
		return personalLibraryService.updateCollection(collectionId, request);
	}

	/** Deletes a personal collection. */
	@DeleteMapping("/collections/{collectionId}")
	@Operation(summary = "Delete personal collection", description = "Permanently delete a collection owned by the current user and its memberships. The underlying posts are unaffected.")
	public ApiResponse<Void> deleteCollection(@Parameter(description = "Collection ID") @PathVariable Long collectionId) {
		return personalLibraryService.deleteCollection(collectionId);
	}

	/** Returns posts stored in a personal collection. */
	@GetMapping("/collections/{collectionId}/posts")
	@Operation(summary = "Get posts in personal collection", description = "Return published posts currently stored in one collection owned by the current user.")
	public ApiResponse<PageResult<CollectionPostResponse>> getCollectionPosts(
			@Parameter(description = "Collection ID") @PathVariable Long collectionId,
			@Parameter(description = "Zero-based request pagination. Responses use a one-based page number.") @PageableDefault(size = 20) Pageable pageable) {
		return personalLibraryService.getCollectionPosts(collectionId, pageable);
	}

	/** Adds a published post to a personal collection. */
	@PostMapping("/collections/{collectionId}/posts/{postId}")
	@Operation(summary = "Add post to personal collection", description = "Add one published post to a collection owned by the current user. Repeating the request does not duplicate membership.")
	public ApiResponse<Void> addPostToCollection(@Parameter(description = "Collection ID") @PathVariable Long collectionId,
			@Parameter(description = "Published post ID") @PathVariable Long postId) {
		return personalLibraryService.addPostToCollection(collectionId, postId);
	}

	/** Removes a post from a personal collection. */
	@DeleteMapping("/collections/{collectionId}/posts/{postId}")
	@Operation(summary = "Remove post from personal collection", description = "Remove one post from a collection owned by the current user. The post and the collection remain available.")
	public ApiResponse<Void> removePostFromCollection(@Parameter(description = "Collection ID") @PathVariable Long collectionId,
			@Parameter(description = "Published post ID") @PathVariable Long postId) {
		return personalLibraryService.removePostFromCollection(collectionId, postId);
	}
}
