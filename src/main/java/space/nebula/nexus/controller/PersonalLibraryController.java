package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
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
@RestController
@RequestMapping("/api/v1/user/library")
@RequiredArgsConstructor
public class PersonalLibraryController
{
	private final IPersonalLibraryService personalLibraryService;

	/**
	 * Returns the current user's personalized library landing content.
	 *
	 * @return aggregated personal content sections
	 */
	@GetMapping("/overview")
	@Operation(summary = "Get personal library overview",
			description = "Returns continue-reading sessions, recent favorites, and explainable recommendations.")
	public ApiResponse<PersonalLibraryOverviewResponse> getOverview()
	{
		return personalLibraryService.getOverview();
	}

	/**
	 * Returns the current user's followed-category content feed.
	 *
	 * @param pageable pagination settings
	 * @return followed-category post page
	 */
	@GetMapping("/following")
	@Operation(summary = "Get followed-category content feed",
			description = "Returns recently published posts from categories explicitly followed by the current user.")
	public ApiResponse<PageResult<PostDigestResponse>> getFollowingFeed(
			@PageableDefault(size = 20) Pageable pageable)
	{
		return personalLibraryService.getFollowingFeed(pageable);
	}

	/**
	 * Returns user-controlled content preferences.
	 *
	 * @return followed categories and hidden recommendation count
	 */
	@GetMapping("/preferences")
	@Operation(summary = "Get content preferences")
	public ApiResponse<ContentPreferenceResponse> getContentPreferences()
	{
		return personalLibraryService.getContentPreferences();
	}

	/**
	 * Follows a category for future recommendations.
	 *
	 * @param categoryId category identifier
	 * @return operation result
	 */
	@PutMapping("/preferences/categories/{categoryId}")
	@Operation(summary = "Follow a recommendation category")
	public ApiResponse<Void> followCategory(@PathVariable Long categoryId)
	{
		return personalLibraryService.followCategory(categoryId);
	}

	/**
	 * Stops following a recommendation category.
	 *
	 * @param categoryId category identifier
	 * @return operation result
	 */
	@DeleteMapping("/preferences/categories/{categoryId}")
	@Operation(summary = "Unfollow a recommendation category")
	public ApiResponse<Void> unfollowCategory(@PathVariable Long categoryId)
	{
		return personalLibraryService.unfollowCategory(categoryId);
	}

	/**
	 * Hides a post from future recommendations.
	 *
	 * @param postId post identifier
	 * @return operation result
	 */
	@PutMapping("/preferences/hidden-posts/{postId}")
	@Operation(summary = "Hide a post from recommendations")
	public ApiResponse<Void> hideRecommendation(@PathVariable Long postId)
	{
		return personalLibraryService.hideRecommendation(postId);
	}

	/**
	 * Restores one hidden recommendation.
	 *
	 * @param postId post identifier
	 * @return operation result
	 */
	@DeleteMapping("/preferences/hidden-posts/{postId}")
	@Operation(summary = "Restore a hidden recommendation")
	public ApiResponse<Void> restoreRecommendation(@PathVariable Long postId)
	{
		return personalLibraryService.restoreRecommendation(postId);
	}

	/**
	 * Clears all hidden recommendation feedback.
	 *
	 * @return operation result
	 */
	@DeleteMapping("/preferences/hidden-posts")
	@Operation(summary = "Clear hidden recommendation feedback")
	public ApiResponse<Void> clearHiddenRecommendations()
	{
		return personalLibraryService.clearHiddenRecommendations();
	}

	/** Returns the current user's favorite posts. */
	@GetMapping("/favorites")
	@Operation(summary = "Get favorite posts")
	public ApiResponse<PageResult<FavoritePostResponse>> getFavorites(@PageableDefault(size = 20) Pageable pageable)
	{
		return personalLibraryService.getFavorites(pageable);
	}

	/** Returns resumable reading history. */
	@GetMapping("/history")
	@Operation(summary = "Get reading history")
	public ApiResponse<PageResult<ReadingHistoryResponse>> getReadingHistory(
			@PageableDefault(size = 20) Pageable pageable)
	{
		return personalLibraryService.getReadingHistory(pageable);
	}

	/** Records reading progress for a published post. */
	@PutMapping("/posts/{postId}/progress")
	@Operation(summary = "Save reading progress")
	public ApiResponse<ReadingHistoryResponse> recordReadingProgress(@PathVariable Long postId,
			@Valid @RequestBody ReadingProgressRequest request)
	{
		return personalLibraryService.recordReadingProgress(postId, request);
	}

	/** Removes one post from reading history. */
	@DeleteMapping("/history/{postId}")
	@Operation(summary = "Remove reading history entry")
	public ApiResponse<Void> deleteReadingHistory(@PathVariable Long postId)
	{
		return personalLibraryService.deleteReadingHistory(postId);
	}

	/** Clears all reading history owned by the current user. */
	@DeleteMapping("/history")
	@Operation(summary = "Clear reading history")
	public ApiResponse<Void> clearReadingHistory()
	{
		return personalLibraryService.clearReadingHistory();
	}

	/** Returns personal collection summaries. */
	@GetMapping("/collections")
	@Operation(summary = "Get personal collections")
	public ApiResponse<List<PostCollectionResponse>> getCollections()
	{
		return personalLibraryService.getCollections();
	}

	/** Creates a personal collection. */
	@PostMapping("/collections")
	@Operation(summary = "Create personal collection")
	public ApiResponse<PostCollectionResponse> createCollection(@Valid @RequestBody PostCollectionRequest request)
	{
		return personalLibraryService.createCollection(request);
	}

	/** Updates a personal collection owned by the current user. */
	@PutMapping("/collections/{collectionId}")
	@Operation(summary = "Update personal collection")
	public ApiResponse<PostCollectionResponse> updateCollection(@PathVariable Long collectionId,
			@Valid @RequestBody PostCollectionRequest request)
	{
		return personalLibraryService.updateCollection(collectionId, request);
	}

	/** Deletes a personal collection. */
	@DeleteMapping("/collections/{collectionId}")
	@Operation(summary = "Delete personal collection")
	public ApiResponse<Void> deleteCollection(@PathVariable Long collectionId)
	{
		return personalLibraryService.deleteCollection(collectionId);
	}

	/** Returns posts stored in a personal collection. */
	@GetMapping("/collections/{collectionId}/posts")
	@Operation(summary = "Get posts in personal collection")
	public ApiResponse<PageResult<CollectionPostResponse>> getCollectionPosts(@PathVariable Long collectionId,
			@PageableDefault(size = 20) Pageable pageable)
	{
		return personalLibraryService.getCollectionPosts(collectionId, pageable);
	}

	/** Adds a published post to a personal collection. */
	@PostMapping("/collections/{collectionId}/posts/{postId}")
	@Operation(summary = "Add post to personal collection")
	public ApiResponse<Void> addPostToCollection(@PathVariable Long collectionId, @PathVariable Long postId)
	{
		return personalLibraryService.addPostToCollection(collectionId, postId);
	}

	/** Removes a post from a personal collection. */
	@DeleteMapping("/collections/{collectionId}/posts/{postId}")
	@Operation(summary = "Remove post from personal collection")
	public ApiResponse<Void> removePostFromCollection(@PathVariable Long collectionId, @PathVariable Long postId)
	{
		return personalLibraryService.removePostFromCollection(collectionId, postId);
	}
}
