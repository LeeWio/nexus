package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostAutosaveResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;

public interface IPostService
{
	ApiResponse<PageResult<PostResponse>> searchPostsForAdmin(PostStatus status, Long categoryId, String keyword, Pageable pageable);

	ApiResponse<PostResponse> retrievePostById(Long id);

	ApiResponse<PostResponse> createPost(PostRequest request);

	ApiResponse<PostResponse> updatePost(Long id, PostRequest request);

	ApiResponse<Void> deletePost(Long id);

	/**
	 * Submits a draft post for review.
	 */
	ApiResponse<Void> submitForReview(Long id);

	/**
	 * Withdraws a pending post so its author can revise it.
	 *
	 * @param id post identifier
	 * @return operation result
	 */
	ApiResponse<Void> withdrawFromReview(Long id);

	/**
	 * Approves or rejects a pending post.
	 */
	ApiResponse<Void> reviewPost(Long id, boolean approved, String reviewComment);

	/**
	 * Approves a pending post for publication at a future time.
	 *
	 * @param id post identifier
	 * @param request requested publication time
	 * @return operation result
	 */
	ApiResponse<Void> schedulePost(Long id, PostScheduleRequest request);

	/**
	 * Cancels a scheduled publication and returns the post to editorial review.
	 *
	 * @param id post identifier
	 * @return operation result
	 */
	ApiResponse<Void> cancelScheduledPost(Long id);

	/**
	 * Removes a published post from public visibility with an audit reason.
	 *
	 * @param id post identifier
	 * @param request archive reason
	 * @return operation result
	 */
	ApiResponse<Void> archivePost(Long id, PostArchiveRequest request);

	/**
	 * Returns an archived post to draft status for revision and review.
	 *
	 * @param id post identifier
	 * @return operation result
	 */
	ApiResponse<Void> restoreArchivedPost(Long id);

	/**
	 * Publishes a bounded batch of scheduled posts whose publication time has
	 * arrived.
	 *
	 * @param now publication cutoff
	 * @param batchSize maximum number of posts to publish
	 * @return number of published posts
	 */
	int publishDueScheduledPosts(java.time.LocalDateTime now, int batchSize);

	// Public methods
	ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Pageable pageable);

	ApiResponse<PostResponse> retrievePostBySlug(String slug);

	/**
	 * Returns a compact, curated set of posts for the public discovery page.
	 *
	 * @return spotlight, latest, and most-read post groups
	 */
	ApiResponse<BlogDiscoveryResponse> retrievePublicDiscovery();

	/**
	 * Temporarily saves post content to Redis to prevent data loss.
	 */
	ApiResponse<Void> autosavePostContent(PostAutosaveRequest request);

	/**
	 * Retrieves temporarily saved content from Redis.
	 */
	ApiResponse<PostAutosaveResponse> retrieveAutosavedContent(String identifier);
}
