package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostRevisionResponse;
import space.nebula.nexus.service.IPostService;
import space.nebula.nexus.service.IPostRevisionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/posts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Blog Management", description = "Endpoints for managing blog posts (Requires ADMIN role)")
public class AdminPostController {

    private final IPostService postService;
    private final IPostRevisionService postRevisionService;

    @GetMapping
    @Operation(summary = "Get all posts (paginated)")
    public ApiResponse<PageResult<PostResponse>> getAllPosts(@PageableDefault(size = 10) Pageable pageable) {
        return postService.getAdminPosts(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long id) {
        return postService.getPostById(id);
    }

    @PostMapping
    @Operation(summary = "Create a new post")
    public ApiResponse<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
        return postService.createPost(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a post")
    public ApiResponse<PostResponse> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        return postService.updatePost(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post")
    public ApiResponse<Void> deletePost(@PathVariable Long id) {
        return postService.deletePost(id);
    }

    @GetMapping("/{id}/revisions")
    @Operation(summary = "Get revision history for a post")
    public ApiResponse<List<PostRevisionResponse>> getPostRevisions(@PathVariable Long id) {
        return postRevisionService.getPostRevisions(id);
    }

    @PostMapping("/{id}/revisions/{revisionId}/revert")
    @Operation(summary = "Revert a post to a specific revision")
    public ApiResponse<PostResponse> revertToRevision(@PathVariable Long id, @PathVariable Long revisionId) {
        return postRevisionService.revertToRevision(id, revisionId);
    }
}
