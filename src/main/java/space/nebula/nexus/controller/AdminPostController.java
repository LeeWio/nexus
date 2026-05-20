package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import space.nebula.nexus.payload.response.PostDiffResponse;
import space.nebula.nexus.payload.response.PostResponse;
import space.nebula.nexus.payload.response.PostRevisionResponse;
import space.nebula.nexus.service.IPostRevisionService;
import space.nebula.nexus.service.IPostService;

import java.util.List;

/**
 * Controller for administrative blog post management. Handles CRUD operations,
 * autosave, and content revisions.
 */
@Tag(name = "Admin Post Management", description = "Endpoints for authors to manage blog posts")
@RestController
@RequestMapping("/api/v1/admin/posts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPostController {

	private final IPostService postService;
	private final IPostRevisionService postRevisionService;

	@GetMapping
	@Operation(summary = "Search all posts (Management)", description = "Returns a paginated list of all posts, including drafts and scheduled.")
	public ApiResponse<PageResult<PostResponse>> searchPosts(
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 10) Pageable pageable) {
		return postService.searchPostsForAdmin(pageable);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Retrieve post by ID", description = "Fetch complete post details for editing.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")})
	public ApiResponse<PostResponse> retrievePost(
			@Parameter(description = "Unique ID of the post", example = "1") @PathVariable Long id) {
		return postService.retrievePostById(id);
	}

	@PostMapping
	@Operation(summary = "Create a new post", description = "Initializes a new blog post with provided metadata and content.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post created successfully")
	public ApiResponse<PostResponse> createPost(@Valid @RequestBody PostRequest request) {
		return postService.createPost(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update an existing post", description = "Saves changes to an existing post and creates a new revision.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post updated"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")})
	public ApiResponse<PostResponse> updatePost(
			@Parameter(description = "ID of the post to update") @PathVariable Long id,
			@Valid @RequestBody PostRequest request) {
		return postService.updatePost(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a post", description = "Permanently removes a post and all its revisions.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post deleted")
	public ApiResponse<Void> deletePost(@Parameter(description = "ID of the post to delete") @PathVariable Long id) {
		return postService.deletePost(id);
	}

	@PostMapping("/autosave")
	@Operation(summary = "Autosave content", description = "Non-persistent saving of content to Redis to prevent data loss during editing.")
	public ApiResponse<Void> autosavePost(@Valid @RequestBody PostAutosaveRequest request) {
		return postService.autosavePostContent(request);
	}

	@GetMapping("/autosave/{identifier}")
	@Operation(summary = "Retrieve autosaved content", description = "Get the last autosaved version using the identifier (ID or UUID).")
	public ApiResponse<String> retrieveAutosave(
			@Parameter(description = "Identifier for the autosave session") @PathVariable String identifier) {
		return postService.retrieveAutosavedContent(identifier);
	}

	@GetMapping("/{id}/revisions")
	@Operation(summary = "List revisions", description = "Returns the history of all saved versions for a specific post.")
	public ApiResponse<List<PostRevisionResponse>> retrieveRevisions(
			@Parameter(description = "ID of the post") @PathVariable Long id) {
		return postRevisionService.getPostRevisions(id);
	}

	@PostMapping("/{id}/revisions/{revisionId}/revert")
	@Operation(summary = "Revert to revision", description = "Restores the post content and metadata from a previous revision.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reversion successful"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post or Revision not found")})
	public ApiResponse<PostResponse> revertToRevision(@Parameter(description = "ID of the post") @PathVariable Long id,
			@Parameter(description = "ID of the specific revision") @PathVariable Long revisionId) {
		return postRevisionService.revertToRevision(id, revisionId);
	}

	@GetMapping("/{id}/revisions/compare")
	@Operation(summary = "Compare revisions", description = "Compare two historical revisions of a post to see field-level differences.")
	public ApiResponse<PostDiffResponse> compareRevisions(@Parameter(description = "Post ID") @PathVariable Long id,
			@Parameter(description = "Base revision ID") @RequestParam Long baseId,
			@Parameter(description = "Target revision ID") @RequestParam Long targetId) {
		return postRevisionService.compareRevisions(id, baseId, targetId);
	}
}
