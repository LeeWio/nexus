package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;
import space.nebula.nexus.payload.response.BlogFacetResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.PostResponse;

import java.util.List;

public interface IPublicPostQueryService {
	ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Pageable pageable);

	ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String keyword,
			Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable);

	ApiResponse<PageResult<PostResponse>> searchPublicPosts(Long categoryId, Long tagId, String authorName,
			String keyword, Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable);

	ApiResponse<PageResult<PostDigestResponse>> searchPublicPostDigests(Long categoryId, Long tagId, String keyword,
			Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable);

	ApiResponse<PageResult<PostDigestResponse>> searchPublicPostDigests(Long categoryId, Long tagId, String authorName,
			String keyword, Boolean featuredOnly, Boolean hasCover, PostContentType contentType, Pageable pageable);

	ApiResponse<PageResult<PostDigestResponse>> retrievePublicArchive(Integer year, Integer month, Pageable pageable);

	ApiResponse<BlogFacetResponse> retrievePublicFacets();

	ApiResponse<PageResult<PostDigestResponse>> retrieveFeaturedPublicPosts(Pageable pageable);

	ApiResponse<BlogDiscoveryResponse> retrievePublicDiscovery();

	ApiResponse<PostResponse> retrievePostBySlug(String slug);

	ApiResponse<List<PostDigestResponse>> retrieveRelatedPosts(String slug, Pageable pageable);

	ApiResponse<String> createPreviewToken(Long id);

	ApiResponse<PostResponse> retrievePostPreview(String token);
}
