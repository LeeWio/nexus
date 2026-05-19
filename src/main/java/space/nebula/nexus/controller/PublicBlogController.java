package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.service.IPostService;

@RestController
@RequestMapping("/api/v1/public/blog")
@Tag(name = "Public Blog API", description = "Public endpoints for reading blog posts")
public class PublicBlogController {

    @Resource
    private IPostService postService;

    @GetMapping("/posts")
    @Operation(summary = "Search published posts", description = "Browse all published posts with filtering by category, tag, or keyword.")
    public space.nebula.nexus.common.ApiResponse<PageResult<PostResponse>> searchPosts(
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by tag ID") @RequestParam(required = false) Long tagId,
            @Parameter(description = "Search in title and content") @RequestParam(required = false) String keyword,
            @Parameter(description = "Pagination and sorting") @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return postService.searchPublicPosts(categoryId, tagId, keyword, pageable);
    }

    @GetMapping("/posts/{slug}")
    @Operation(summary = "Retrieve post by slug", description = "Fetch the full content of a published post using its unique URL slug.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Post found"),
        @ApiResponse(responseCode = "404", description = "Post not found")
    })
    public space.nebula.nexus.common.ApiResponse<PostResponse> retrievePost(
            @Parameter(description = "The unique URL slug of the post", example = "my-awesome-post") @PathVariable String slug) {
        return postService.retrievePostBySlug(slug);
    }
}
