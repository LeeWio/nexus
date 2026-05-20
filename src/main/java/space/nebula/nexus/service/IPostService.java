package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;

public interface IPostService {
	ApiResponse<PageResult<PostResponse>> searchPostsForAdmin(Pageable pageable);
	ApiResponse<PostResponse> retrievePostById(Long id);
	ApiResponse<PostResponse> createPost(PostRequest request);
	ApiResponse<PostResponse> updatePost(Long id, PostRequest request);
	ApiResponse<Void> deletePost(Long id);

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
	ApiResponse<String> retrieveAutosavedContent(String identifier);
}
