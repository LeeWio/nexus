package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;

public interface IPostService {
    ApiResponse<PageResult<PostResponse>> getAdminPosts(Pageable pageable);
    ApiResponse<PostResponse> getPostById(Long id);
    ApiResponse<PostResponse> createPost(PostRequest request);
    ApiResponse<PostResponse> updatePost(Long id, PostRequest request);
    ApiResponse<Void> deletePost(Long id);

    // Public methods
    ApiResponse<PageResult<PostResponse>> getPublishedPosts(Long categoryId, Long tagId, String keyword, Pageable pageable);
    ApiResponse<PostResponse> getPostBySlug(String slug);
}
