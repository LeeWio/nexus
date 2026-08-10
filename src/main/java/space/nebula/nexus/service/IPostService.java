package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.BatchDeleteRequest;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.BlogFacetResponse;
import space.nebula.nexus.payload.response.PostAutosaveResponse;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;

import java.util.List;

public interface IPostService {
	ApiResponse<PageResult<PostResponse>> searchPostsForAdmin(PostStatus status, Long categoryId, String keyword,
			Pageable pageable);

	ApiResponse<PostResponse> retrievePostById(Long id);

	ApiResponse<PostResponse> createPost(PostRequest request);

	ApiResponse<PostResponse> updatePost(Long id, PostRequest request);

	/**
	 * Updates a post and rejects the write when the client is editing a stale
	 * revision. A {@code null} expected revision keeps legacy clients compatible.
	 */
	ApiResponse<PostResponse> updatePost(Long id, PostRequest request, Integer expectedRevisionNumber);

	ApiResponse<Void> deletePost(Long id);

	ApiResponse<Void> deletePosts(BatchDeleteRequest request);

	ApiResponse<PostResponse> copyPost(Long id);

	/**
	 * Submits a draft post for review.
	 */
	ApiResponse<Void> submitForReview(Long id);

	/**
	 * Withdraws a pending post so its author can revise it.
	 *
	 * @param id
	 *            post identifier
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
	 * @param id
	 *            post identifier
	 * @param request
	 *            requested publication time
	 * @return operation result
	 */
	ApiResponse<Void> schedulePost(Long id, PostScheduleRequest request);

	/**
	 * Cancels a scheduled publication and returns the post to editorial review.
	 *
	 * @param id
	 *            post identifier
	 * @return operation result
	 */
	ApiResponse<Void> cancelScheduledPost(Long id);

	/**
	 * Removes a published post from public visibility with an audit reason.
	 *
	 * @param id
	 *            post identifier
	 * @param request
	 *            archive reason
	 * @return operation result
	 */
	ApiResponse<Void> archivePost(Long id, PostArchiveRequest request);

	/**
	 * Returns an archived post to draft status for revision and review.
	 *
	 * @param id
	 *            post identifier
	 * @return operation result
	 */
	ApiResponse<Void> restoreArchivedPost(Long id);

	/**
	 * Publishes a bounded batch of scheduled posts whose publication time has
	 * arrived.
	 *
	 * @param now
	 *            publication cutoff
	 * @param batchSize
	 *            maximum number of posts to publish
	 * @return number of published posts
	 */
	int publishDueScheduledPosts(java.time.LocalDateTime now, int batchSize);

	// Public methods
	ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Pageable pageable);

	ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable);

	ApiResponse<PageResult<PostDigestResponse>> searchPublicPostDigests(Long categoryId, Long tagId, String keyword,
			Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable);

	ApiResponse<PageResult<PostDigestResponse>> retrievePublicArchive(Integer year, Integer month, Pageable pageable);

	ApiResponse<BlogFacetResponse> retrievePublicFacets();

	/**
	 * Returns only prominent published posts for public listing surfaces.
	 */
	ApiResponse<PageResult<PostDigestResponse>> retrieveFeaturedPublicPosts(Pageable pageable);

	ApiResponse<PostResponse> retrievePostBySlug(String slug);

	ApiResponse<List<PostDigestResponse>> retrieveRelatedPosts(String slug, Pageable pageable);

	ApiResponse<String> createPreviewToken(Long id);

	ApiResponse<PostResponse> retrievePostPreview(String token);

	ApiResponse<Integer> rebuildPostContentMetadata();

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
