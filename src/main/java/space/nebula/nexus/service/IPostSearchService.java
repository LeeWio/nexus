package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.entity.document.PostDocument;

public interface IPostSearchService {

    /**
     * Index a post into Elasticsearch
     */
    void indexPost(Post post);

    /**
     * Delete a post from Elasticsearch
     */
    void deletePostIndex(Long postId);

    /**
     * Rebuild entire index from database
     */
    void rebuildIndex();

    /**
     * Search posts by keyword
     */
    ApiResponse<PageResult<PostDocument>> searchPosts(String keyword, Pageable pageable);
}
