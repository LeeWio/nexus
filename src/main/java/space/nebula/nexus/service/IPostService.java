package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.BatchDeleteRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostMetadataRebuildResponse;

public interface IPostService {
	ApiResponse<PageResult<PostResponse>> searchPostsForAdmin(PostStatus status, Long categoryId, String keyword,
			Pageable pageable);

	ApiResponse<PostResponse> retrievePostById(Long id);

	ApiResponse<PostResponse> createPost(PostRequest request);

	ApiResponse<PostResponse> createPost(PostRequest request, String editorDraftRevision);

	ApiResponse<PostResponse> updatePost(Long id, PostRequest request);

	/**
	 * Updates a post and rejects the write when the client is editing a stale
	 * revision. A {@code null} expected revision keeps legacy clients compatible.
	 */
	ApiResponse<PostResponse> updatePost(Long id, PostRequest request, Integer expectedRevisionNumber);

	ApiResponse<PostResponse> updatePost(Long id, PostRequest request, Integer expectedRevisionNumber,
			String editorDraftRevision);

	ApiResponse<Void> deletePost(Long id);

	ApiResponse<Void> deletePosts(BatchDeleteRequest request);

	ApiResponse<PostResponse> copyPost(Long id);

	ApiResponse<PostMetadataRebuildResponse> rebuildPostContentMetadata(int batchSize, boolean dryRun);

}
