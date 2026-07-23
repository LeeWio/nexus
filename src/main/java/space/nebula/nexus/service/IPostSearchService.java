package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.payload.response.QuickSearchResponse;
import space.nebula.nexus.payload.response.UnifiedSearchResponse;

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

	/**
	 * Performs a lightweight search across multiple entities for Command+K UI.
	 */
	ApiResponse<QuickSearchResponse> quickSearch(String keyword);

	/**
	 * Performs a professional unified search across all entities with metadata and
	 * Next.js routing.
	 */
	ApiResponse<UnifiedSearchResponse> unifiedSearch(String keyword);

	/**
	 * Provides search suggestions/completions as the user types.
	 */
	ApiResponse<java.util.List<String>> getSearchSuggestions(String keyword);
}
