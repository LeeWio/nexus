package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ICommentService;

/**
 * Controller for administrative comment management. Provides endpoints for
 * moderation, status updates, and cleanup of user comments.
 */
@Tag(name = "Admin Comment Management", description = "Endpoints for moderating and managing user-generated comments")
@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommentController
{

	private final ICommentService commentService;

	@GetMapping
	@Operation(summary = "Search all comments", description = "Retrieve a paginated list of all comments for moderation purposes.")
	public ApiResponse<PageResult<CommentResponse>> searchComments(
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return commentService.searchCommentsForManagement(pageable);
	}

	@GetMapping("/pending")
	@Operation(summary = "Get pending comments", description = "Retrieve a list of comments that are currently awaiting moderator approval.")
	public ApiResponse<PageResult<CommentResponse>> getPendingComments(
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return commentService.retrievePendingComments(pageable);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Moderate comment status", description = "Approve, reject, or mark a comment as spam.")
	public ApiResponse<Void> moderateComment(@Parameter(description = "Comment ID") @PathVariable Long id,
			@Parameter(description = "Target status for the comment") @RequestParam CommentStatus status)
	{
		return commentService.moderateComment(id, status);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete comment", description = "Permanently remove a comment from the system.")
	public ApiResponse<Void> deleteComment(@Parameter(description = "Comment ID") @PathVariable Long id)
	{
		return commentService.deleteComment(id);
	}
}
