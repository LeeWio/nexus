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
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.request.PostAutosaveRequest;
import space.nebula.nexus.payload.request.PostArchiveRequest;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.payload.request.PostScheduleRequest;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.PostAutosaveResponse;
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
public class AdminPostController
{

	private final IPostService postService;
	private final IPostRevisionService postRevisionService;
	private final space.nebula.nexus.service.IStaticGenerationService staticGenerationService;

	@GetMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
	@Operation(summary = "Search all posts (Management)", description = "Returns a paginated list of all posts, including drafts and scheduled.")
	public ApiResponse<PageResult<PostResponse>> searchPosts(
			@Parameter(description = "Filter by post status") @RequestParam(required = false) PostStatus status,
			@Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
			@Parameter(description = "Filter by keyword") @RequestParam(required = false) String keyword,
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 10) Pageable pageable)
	{
		return postService.searchPostsForAdmin(status, categoryId, keyword, pageable);
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasPermission(#id, 'Post', 'READ') or hasAnyRole('ADMIN', 'EDITOR')")
	@Operation(summary = "Retrieve post by ID", description = "Fetch complete post details for editing.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post found"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found") })
	public ApiResponse<PostResponse> retrievePost(
			@Parameter(description = "Unique ID of the post", example = "1") @PathVariable Long id)
	{
		return postService.retrievePostById(id);
	}

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Create a new post", description = "Initializes a new blog post with provided metadata and content.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post created successfully")
	public ApiResponse<PostResponse> createPost(@Valid @RequestBody PostRequest request)
	{
		return postService.createPost(request);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasPermission(#id, 'Post', 'EDIT')")
	@Operation(summary = "Update an existing post", description = "Saves changes to an existing post and creates a new revision.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post updated"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found") })
	public ApiResponse<PostResponse> updatePost(
			@Parameter(description = "ID of the post to update") @PathVariable Long id,
			@Valid @RequestBody PostRequest request)
	{
		return postService.updatePost(id, request);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasPermission(#id, 'Post', 'DELETE')")
	@Operation(summary = "Delete a post", description = "Permanently removes a post and all its revisions.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post deleted")
	public ApiResponse<Void> deletePost(@Parameter(description = "ID of the post to delete") @PathVariable Long id)
	{
		return postService.deletePost(id);
	}

	@PostMapping("/{id}/submit")
	@PreAuthorize("hasPermission(#id, 'Post', 'SUBMIT')")
	@Operation(summary = "Submit for review", description = "Submit a draft post for editorial review.")
	public ApiResponse<Void> submitForReview(@PathVariable Long id)
	{
		return postService.submitForReview(id);
	}

	@PostMapping("/{id}/withdraw")
	@PreAuthorize("hasPermission(#id, 'Post', 'SUBMIT')")
	@Operation(summary = "Withdraw from review", description = "Return a pending post to draft status for revision.")
	public ApiResponse<Void> withdrawFromReview(@PathVariable Long id)
	{
		return postService.withdrawFromReview(id);
	}

	@PostMapping("/{id}/review")
	@PreAuthorize("hasPermission(#id, 'Post', 'APPROVE')")
	@Operation(summary = "Review post", description = "Approve or reject a submitted post.")
	public ApiResponse<Void> reviewPost(@PathVariable Long id,
			@Valid @RequestBody space.nebula.nexus.payload.request.PostReviewRequest request)
	{
		return postService.reviewPost(id, request.approved(), request.reviewComment());
	}

	@PostMapping("/{id}/schedule")
	@PreAuthorize("hasPermission(#id, 'Post', 'APPROVE')")
	@Operation(summary = "Schedule publication", description = "Approve a pending post and schedule it for future publication.")
	public ApiResponse<Void> schedulePost(@PathVariable Long id,
			@Valid @RequestBody PostScheduleRequest request)
	{
		return postService.schedulePost(id, request);
	}

	@DeleteMapping("/{id}/schedule")
	@PreAuthorize("hasPermission(#id, 'Post', 'APPROVE')")
	@Operation(summary = "Cancel scheduled publication", description = "Return a scheduled post to editorial review without publishing it.")
	public ApiResponse<Void> cancelScheduledPost(@PathVariable Long id)
	{
		return postService.cancelScheduledPost(id);
	}

	@PostMapping("/{id}/archive")
	@PreAuthorize("hasPermission(#id, 'Post', 'APPROVE')")
	@Operation(summary = "Archive published post", description = "Remove a published post from public visibility while retaining its audit history.")
	public ApiResponse<Void> archivePost(@PathVariable Long id,
			@Valid @RequestBody PostArchiveRequest request)
	{
		return postService.archivePost(id, request);
	}

	@PostMapping("/{id}/restore")
	@PreAuthorize("hasPermission(#id, 'Post', 'APPROVE')")
	@Operation(summary = "Restore archived post", description = "Return an archived post to draft status for revision and a new review cycle.")
	public ApiResponse<Void> restoreArchivedPost(@PathVariable Long id)
	{
		return postService.restoreArchivedPost(id);
	}

	@PostMapping("/autosave")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Autosave content", description = "Non-persistent saving of content to Redis to prevent data loss during editing.")
	public ApiResponse<Void> autosavePost(@Valid @RequestBody PostAutosaveRequest request)
	{
		return postService.autosavePostContent(request);
	}

	@PostMapping("/{id}/preview-token")
	@PreAuthorize("hasPermission(#id, 'Post', 'READ') or hasAnyRole('ADMIN', 'EDITOR')")
	@Operation(summary = "Create preview token", description = "Creates a short-lived token for previewing a non-public post.")
	public ApiResponse<String> createPreviewToken(@PathVariable Long id)
	{
		return postService.createPreviewToken(id);
	}

	@PostMapping("/rebuild-metadata")
	@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
	@Operation(summary = "Rebuild post metadata", description = "Recomputes summaries, table of contents, content hashes, word counts, and reading time.")
	public ApiResponse<Integer> rebuildPostMetadata()
	{
		return postService.rebuildPostContentMetadata();
	}

	@GetMapping("/autosave/{identifier}")
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Retrieve autosaved content", description = "Get the last autosaved version using the identifier (ID or UUID).")
	public ApiResponse<PostAutosaveResponse> retrieveAutosave(
			@Parameter(description = "Identifier for the autosave session") @PathVariable String identifier)
	{
		return postService.retrieveAutosavedContent(identifier);
	}

	@GetMapping("/{id}/revisions")
	@PreAuthorize("hasPermission(#id, 'Post', 'READ') or hasAnyRole('ADMIN', 'EDITOR')")
	@Operation(summary = "List revisions", description = "Returns the history of all saved versions for a specific post.")
	public ApiResponse<List<PostRevisionResponse>> retrieveRevisions(
			@Parameter(description = "ID of the post") @PathVariable Long id)
	{
		return postRevisionService.getPostRevisions(id);
	}

	@PostMapping("/{id}/revisions/{revisionId}/revert")
	@PreAuthorize("hasPermission(#id, 'Post', 'EDIT')")
	@Operation(summary = "Revert to revision", description = "Restores the post content and metadata from a previous revision.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reversion successful"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post or Revision not found") })
	public ApiResponse<PostResponse> revertToRevision(@Parameter(description = "ID of the post") @PathVariable Long id,
			@Parameter(description = "ID of the specific revision") @PathVariable Long revisionId)
	{
		return postRevisionService.revertToRevision(id, revisionId);
	}

	@GetMapping("/{id}/revisions/compare")
	@PreAuthorize("hasPermission(#id, 'Post', 'READ') or hasAnyRole('ADMIN', 'EDITOR')")
	@Operation(summary = "Compare revisions", description = "Compare two historical revisions of a post to see field-level differences.")
	public ApiResponse<PostDiffResponse> compareRevisions(@Parameter(description = "Post ID") @PathVariable Long id,
			@Parameter(description = "Base revision ID") @RequestParam Long baseId,
			@Parameter(description = "Target revision ID") @RequestParam Long targetId)
	{
		return postRevisionService.compareRevisions(id, baseId, targetId);
	}

	@PostMapping("/regenerate-static")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Regenerate all static HTML", description = "Trigger a background task to rebuild static HTML files for all published posts.")
	public ApiResponse<Void> regenerateStaticHtml()
	{
		staticGenerationService.regenerateAllPosts();
		return ApiResponse.success("Static HTML regeneration task initiated", null);
	}
}
