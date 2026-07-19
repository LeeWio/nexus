package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.BlogDiscoveryResponse;
import space.nebula.nexus.service.IPostService;

/**
 * Controller for public access to blog content. Provides endpoints for browsing
 * and reading published posts.
 */
@Tag(name = "Public Blog API", description = "Public endpoints for reading blog posts")
@RestController
@RequestMapping("/api/v1/public/blog")
@RequiredArgsConstructor
public class PublicBlogController
{

	private final IPostService postService;

	@GetMapping("/discovery")
	@Operation(summary = "Retrieve blog discovery content",
			description = "Returns compact spotlight, latest, and most-read content groups for the public blog experience.")
	public ApiResponse<BlogDiscoveryResponse> retrieveDiscovery()
	{
		return postService.retrievePublicDiscovery();
	}

	@GetMapping("/posts")
	@Operation(summary = "Search published posts", description = "Browse all published posts with filtering by category, tag, or keyword.")
	public ApiResponse<PageResult<PostResponse>> searchPosts(
			@Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,

			@Parameter(description = "Filter by tag ID") @RequestParam(required = false) Long tagId,

			@Parameter(description = "Search in title and content") @RequestParam(required = false) String keyword,

			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 10, sort = "createdAt") Pageable pageable)
	{
		return postService.searchPublicPosts(categoryId, tagId, keyword, pageable);
	}

	@GetMapping("/posts/{slug}")
	@Operation(summary = "Retrieve post by slug", description = "Fetch the full content of a published post using its unique URL slug.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found") })
	public ApiResponse<PostResponse> retrievePost(
			@Parameter(description = "The unique URL slug of the post", example = "my-awesome-post") @PathVariable String slug)
	{
		return postService.retrievePostBySlug(slug);
	}
}
