package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.BatchModerateCommentRequest;
import space.nebula.nexus.payload.response.CommentGovernanceOverviewResponse;
import space.nebula.nexus.payload.response.CommentModerationLogResponse;
import space.nebula.nexus.payload.response.CommentRiskResponse;
import space.nebula.nexus.payload.response.CommentReportResponse;
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
	@Operation(summary = "Search all comments", description = "Retrieve a paginated list of all comments for moderation purposes with filters.")
	public ApiResponse<PageResult<CommentResponse>> searchComments(
			@Parameter(description = "Filter by comment status") @RequestParam(required = false) CommentStatus status,
			@Parameter(description = "Filter by post ID") @RequestParam(required = false) Long postId,
			@Parameter(description = "Filter by username") @RequestParam(required = false) String username,
			@Parameter(description = "Filter by keyword in content") @RequestParam(required = false) String keyword,
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return commentService.searchCommentsForManagement(status, postId, username, keyword, pageable);
	}

	@GetMapping("/pending")
	@Operation(summary = "Get pending comments", description = "Retrieve a list of comments that are currently awaiting moderator approval.")
	public ApiResponse<PageResult<CommentResponse>> getPendingComments(
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return commentService.retrievePendingComments(pageable);
	}

	@GetMapping("/overview")
	@Operation(summary = "Get comment governance overview", description = "Retrieve moderation dashboard counters and recent risk signals.")
	public ApiResponse<CommentGovernanceOverviewResponse> getCommentGovernanceOverview()
	{
		return commentService.retrieveCommentGovernanceOverview();
	}

	@GetMapping("/reports")
	@Operation(summary = "Get comment reports", description = "Retrieve the moderation report queue with status and reporter filters.")
	public ApiResponse<PageResult<CommentReportResponse>> getCommentReports(
			@Parameter(description = "Filter by report status") @RequestParam(required = false) CommentReportStatus status,
			@Parameter(description = "Filter by comment ID") @RequestParam(required = false) Long commentId,
			@Parameter(description = "Filter by reporter username") @RequestParam(required = false) String reporterUsername,
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return commentService.retrieveCommentReports(status, commentId, reporterUsername, pageable);
	}

	@GetMapping("/high-risk")
	@Operation(summary = "Get high-risk comments", description = "Retrieve comments prioritized by open report count and risk score.")
	public ApiResponse<PageResult<CommentRiskResponse>> getHighRiskComments(
			@Parameter(description = "Minimum open reports required to enter the queue") @RequestParam(required = false) Long minOpenReports,
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable)
	{
		return commentService.retrieveHighRiskComments(minOpenReports, pageable);
	}

	@GetMapping("/moderation-logs")
	@Operation(summary = "Get comment moderation logs", description = "Retrieve immutable moderation audit history.")
	public ApiResponse<PageResult<CommentModerationLogResponse>> getCommentModerationLogs(
			@Parameter(description = "Filter by comment ID") @RequestParam(required = false) Long commentId,
			@Parameter(description = "Filter by moderation action") @RequestParam(required = false) CommentModerationAction action,
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return commentService.retrieveCommentModerationLogs(commentId, action, pageable);
	}

	@PostMapping("/repair-counters")
	@Operation(summary = "Repair comment counters", description = "Rebuild denormalized like and report counters from source interaction tables.")
	public ApiResponse<Integer> repairCommentCounters()
	{
		return commentService.repairCommentCounters();
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Moderate comment status", description = "Approve, reject, or mark a comment as spam.")
	public ApiResponse<Void> moderateComment(@Parameter(description = "Comment ID") @PathVariable Long id,
			@Parameter(description = "Target status for the comment") @RequestParam CommentStatus status)
	{
		return commentService.moderateComment(id, status);
	}

	@PostMapping("/batch/status")
	@Operation(summary = "Batch moderate comments", description = "Apply one moderation decision to multiple comments.")
	public ApiResponse<Integer> batchModerateComments(@Valid @RequestBody BatchModerateCommentRequest request)
	{
		return commentService.batchModerateComments(request.ids(), request.status());
	}

	@PatchMapping("/{id}/pin")
	@Operation(summary = "Pin comment", description = "Pin or unpin a comment in public comment ordering.")
	public ApiResponse<Void> pinComment(@Parameter(description = "Comment ID") @PathVariable Long id,
			@Parameter(description = "Pinned state") @RequestParam boolean pinned)
	{
		return commentService.pinComment(id, pinned);
	}

	@PatchMapping("/{id}/feature")
	@Operation(summary = "Feature comment", description = "Mark or unmark a comment as featured in public comment ordering.")
	public ApiResponse<Void> featureComment(@Parameter(description = "Comment ID") @PathVariable Long id,
			@Parameter(description = "Featured state") @RequestParam boolean featured)
	{
		return commentService.featureComment(id, featured);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete comment", description = "Archive a comment that has no active replies.")
	public ApiResponse<Void> deleteComment(@Parameter(description = "Comment ID") @PathVariable Long id)
	{
		return commentService.deleteComment(id);
	}
}
