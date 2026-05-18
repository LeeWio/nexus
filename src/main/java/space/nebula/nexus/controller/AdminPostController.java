package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.service.IPostRevisionService;
import space.nebula.nexus.service.IPostService;

@Tag(name = "Admin Post Management", description = "Endpoints for authors to manage blog posts")
@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

    private final IPostService postService;
    private final IPostRevisionService postRevisionService;

    @GetMapping
    @Operation(summary = "Search all posts (Management)")
    public ApiResponse<PageResult<PostResponse>> searchPosts(@PageableDefault(size = 10) Pageable pageable) {
        return postService.searchPostsForAdmin(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve post by ID")
    public ApiResponse<PostResponse> retrievePost(@PathVariable Long id) {
        return postService.retrievePostById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new post")
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
        return postService.createPost(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing post")
    public ApiResponse<PostResponse> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        return postService.deletePost(id);
    }

    @PostMapping("/autosave")
    @Operation(summary = "Temporarily save post content (Redis-backed)")
    public ApiResponse<Void> autosavePost(@Valid @RequestBody PostAutosaveRequest request) {
        return postService.autosavePostContent(request);
    }

    @GetMapping("/autosave/{identifier}")
    @Operation(summary = "Retrieve autosaved content")
    public ApiResponse<String> retrieveAutosave(@PathVariable String identifier) {
        return postService.retrieveAutosavedContent(identifier);
    }

    @GetMapping("/{id}/revisions")
    @Operation(summary = "Retrieve revision history for a post")
    public ApiResponse<java.util.List<space.nebula.nexus.payload.response.PostRevisionResponse>> retrieveRevisions(
            @PathVariable Long id) {
        return postRevisionService.getPostRevisions(id);
    }

    @PostMapping("/{id}/revisions/{revisionId}/revert")
    @Operation(summary = "Revert a post to a specific revision")
    public ApiResponse<PostResponse> revertToRevision(@PathVariable Long id, @PathVariable Long revisionId) {
        return postRevisionService.revertToRevision(id, revisionId);
    }
}
