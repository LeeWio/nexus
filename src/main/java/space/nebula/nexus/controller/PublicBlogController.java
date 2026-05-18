package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
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
    @Operation(summary = "Search published posts with filtering")
    public ApiResponse<PageResult<PostResponse>> searchPosts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return postService.searchPublicPosts(categoryId, tagId, keyword, pageable);
    }

    @GetMapping("/posts/{slug}")
    @Operation(summary = "Retrieve post details by slug")
    public ApiResponse<PostResponse> retrievePost(@PathVariable String slug) {
        return postService.retrievePostBySlug(slug);
    }
}
