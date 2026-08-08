package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.payload.request.PostReportRequest;
import space.nebula.nexus.payload.response.BlogFacetResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostDigestResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;
import space.nebula.nexus.service.IPostService;
import space.nebula.nexus.service.IPostReportService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controller for public access to blog content. Provides endpoints for browsing
 * and reading published posts.
 */
@Tag(name = "Public Blog API", description = "Public endpoints for reading blog posts")
@RestController
@RequestMapping("/api/v1/public/blog")
@RequiredArgsConstructor
public class PublicBlogController {

	private final IPostService postService;
	private final IPostReportService postReportService;

	@GetMapping("/discovery")
	@Operation(summary = "Retrieve blog discovery content", description = "Returns compact spotlight, latest, and most-read content groups for the public blog experience.")
	public ApiResponse<BlogDiscoveryResponse> retrieveDiscovery() {
		return postService.retrievePublicDiscovery();
	}

	@GetMapping("/posts")
	@Operation(summary = "Search published posts", description = "Browse all published posts with filtering by category, tag, or keyword.")
	public ApiResponse<PageResult<PostResponse>> searchPosts(
			@Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,

			@Parameter(description = "Filter by tag ID") @RequestParam(required = false) Long tagId,

			@Parameter(description = "Search in title and content") @RequestParam(required = false) String keyword,

			@Parameter(description = "Only return editorially featured posts") @RequestParam(required = false) Boolean featuredOnly,

			@Parameter(description = "Only return posts with a cover image") @RequestParam(required = false) Boolean hasCover,

			@Parameter(description = "Filter by content format") @RequestParam(required = false) PostContentType contentType,

			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return postService.searchPublicPosts(categoryId, tagId, keyword, featuredOnly, hasCover, contentType, pageable);
	}

	@GetMapping("/posts/featured")
	@Operation(summary = "Retrieve prominent published posts", description = "Returns only the posts selected by editorial and engagement ranking for listing surfaces.")
	public ApiResponse<PageResult<PostDigestResponse>> retrieveFeaturedPosts(
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 12) Pageable pageable) {
		return postService.retrieveFeaturedPublicPosts(pageable);
	}

	@GetMapping("/posts/digest")
	@Operation(summary = "Search published post digests", description = "Browse published posts without returning full body content.")
	public ApiResponse<PageResult<PostDigestResponse>> searchPostDigests(
			@Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
			@Parameter(description = "Filter by tag ID") @RequestParam(required = false) Long tagId,
			@Parameter(description = "Search in title and content") @RequestParam(required = false) String keyword,
			@Parameter(description = "Only return editorially featured posts") @RequestParam(required = false) Boolean featuredOnly,
			@Parameter(description = "Only return posts with a cover image") @RequestParam(required = false) Boolean hasCover,
			@Parameter(description = "Filter by content format") @RequestParam(required = false) PostContentType contentType,
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return postService.searchPublicPostDigests(categoryId, tagId, keyword, featuredOnly, hasCover, contentType,
				pageable);
	}

	@GetMapping("/archive")
	@Operation(summary = "Retrieve archived published posts", description = "Browse published post digests by publication year and optional month.")
	public ApiResponse<PageResult<PostDigestResponse>> retrieveArchive(
			@Parameter(description = "Publication year") @RequestParam(required = false) Integer year,
			@Parameter(description = "Publication month from 1 to 12") @RequestParam(required = false) Integer month,
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return postService.retrievePublicArchive(year, month, pageable);
	}

	@GetMapping("/facets")
	@Operation(summary = "Retrieve blog facets", description = "Returns counts by category, tag, archive month, and content format.")
	public ApiResponse<BlogFacetResponse> retrieveFacets() {
		return postService.retrievePublicFacets();
	}

	@GetMapping("/posts/{slug}/related")
	@Operation(summary = "Retrieve related posts", description = "Returns posts related by series, category, tags, and public ranking signals.")
	public ApiResponse<List<PostDigestResponse>> retrieveRelatedPosts(
			@Parameter(description = "The unique URL slug of the source post") @PathVariable String slug,
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 6) Pageable pageable) {
		return postService.retrieveRelatedPosts(slug, pageable);
	}

	@PostMapping("/posts/{id}/report")
	@PreAuthorize("isAuthenticated()")
	@RateLimit(count = 5, time = 30, unit = TimeUnit.MINUTES, message = "Too many reports. Please try again later.")
	@Operation(summary = "Report a post", description = "Report a published article for moderator review.")
	public ApiResponse<Void> reportPost(@Parameter(description = "Post ID") @PathVariable Long id,
			@Valid @RequestBody PostReportRequest request) {
		return postReportService.reportPost(id, request);
	}

	@GetMapping("/posts/{slug}")
	@Operation(summary = "Retrieve post by slug", description = "Fetch the full content of a published post using its unique URL slug.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")})
	public ApiResponse<PostResponse> retrievePost(
			@Parameter(description = "The unique URL slug of the post", example = "my-awesome-post") @PathVariable String slug) {
		return postService.retrievePostBySlug(slug);
	}

	@GetMapping("/preview/{token}")
	@Operation(summary = "Preview a post by token", description = "Fetch a short-lived preview of a draft, rejected, scheduled, or published post.")
	public ApiResponse<PostResponse> retrievePreview(@PathVariable String token) {
		return postService.retrievePostPreview(token);
	}
}
