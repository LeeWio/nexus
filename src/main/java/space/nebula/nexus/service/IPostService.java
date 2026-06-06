package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostAutosaveResponse;
import space.nebula.nexus.payload.response.PostResponse;

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
	 * Approves or rejects a pending post.
	 */
	ApiResponse<Void> reviewPost(Long id, boolean approved, String reviewComment);

	// Public methods
	ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Pageable pageable);

	ApiResponse<PostResponse> retrievePostBySlug(String slug);

	/**
	 * Temporarily saves post content to Redis to prevent data loss.
	 */
	ApiResponse<Void> autosavePostContent(PostAutosaveRequest request);

	/**
	 * Retrieves temporarily saved content from Redis.
	 */
	ApiResponse<PostAutosaveResponse> retrieveAutosavedContent(String identifier);
}
