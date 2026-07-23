package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.CommentUpdateRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ICommentService;

@Tag(name = "User Comments", description = "Endpoints for users to manage their own comments")
@RestController
@RequestMapping("/api/v1/user/comments")
@RequiredArgsConstructor
public class UserCommentController
{
	private final ICommentService commentService;

	@GetMapping
	@Operation(summary = "Get my comments", description = "Retrieve comments authored by the current user with optional status filtering.")
	public ApiResponse<PageResult<CommentResponse>> retrieveMyComments(
			@Parameter(description = "Optional comment status filter") @RequestParam(required = false) CommentStatus status,
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return commentService.retrieveMyComments(status, pageable);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete my comment", description = "Delete a comment authored by the current user when it has no child replies.")
	public ApiResponse<Void> deleteMyComment(@Parameter(description = "Comment ID") @PathVariable Long id)
	{
		return commentService.deleteMyComment(id);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Edit my comment", description = "Update a comment authored by the current user and submit it for moderation again.")
	public ApiResponse<Void> updateMyComment(@Parameter(description = "Comment ID") @PathVariable Long id,
			@Valid @RequestBody CommentUpdateRequest request)
	{
		return commentService.updateMyComment(id, request);
	}
}
