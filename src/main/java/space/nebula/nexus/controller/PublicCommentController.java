package space.nebula.nexus.controller;

import cn.hutool.core.lang.tree.Tree;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.request.CommentReportRequest;
import space.nebula.nexus.payload.response.CommentAnchorContextResponse;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.CursorPageResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ICommentService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controller for public comment operations. Allows readers to view and submit
 * comments on blog posts.
 */
@Tag(name = "Public Comment API", description = "Public endpoints for viewing and submitting user comments")
@RestController
@RequestMapping("/api/v1/public/comments")
@RequiredArgsConstructor
public class PublicCommentController
{

	private final ICommentService commentService;

	@PostMapping
	@Operation(summary = "Publish a new comment", description = "Submit a comment on a blog post. Requires user authentication.")
	@PreAuthorize("isAuthenticated()")
	@RateLimit(count = 5, time = 15, unit = TimeUnit.MINUTES, message = "Too many comments. Please wait a moment.")
	public ApiResponse<Void> publishComment(@Valid @RequestBody CommentRequest request,
			HttpServletRequest servletRequest)
	{
		return commentService.publishComment(request, servletRequest);
	}

	@GetMapping("/post/{postId}")
	@Operation(summary = "Retrieve comments for a post", description = "Fetch a complete hierarchical tree of approved comments for a specific post.")
	public ApiResponse<List<Tree<Long>>> retrieveComments(@Parameter(description = "Post ID") @PathVariable Long postId)
	{
		return commentService.retrieveCommentsByPost(postId);
	}

	@GetMapping("/post/{postId}/roots")
	@Operation(summary = "Retrieve root comments for a post", description = "Fetch approved top-level comments for a specific post with pagination.")
	public ApiResponse<PageResult<CommentResponse>> retrieveRootComments(
			@Parameter(description = "Post ID") @PathVariable Long postId,
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return commentService.retrieveRootCommentsByPost(postId, pageable);
	}

	@GetMapping("/post/{postId}/roots/cursor")
	@Operation(summary = "Cursor-load root comments for a post", description = "Fetch approved top-level comments using a stable cursor for infinite scrolling.")
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveRootCommentsCursor(
			@Parameter(description = "Post ID") @PathVariable Long postId,
			@Parameter(description = "Last seen comment ID from the previous response") @RequestParam(required = false) Long cursor,
			@Parameter(description = "Number of comments to return") @RequestParam(defaultValue = "20") int size)
	{
		return commentService.retrieveRootCommentsByPostCursor(postId, cursor, size);
	}

	@GetMapping("/post/{postId}/roots/hot")
	@Operation(summary = "Retrieve hot root comments for a post", description = "Fetch approved top-level comments sorted by pinned, featured, likes, and recency.")
	public ApiResponse<PageResult<CommentResponse>> retrieveHotRootComments(
			@Parameter(description = "Post ID") @PathVariable Long postId,
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable)
	{
		return commentService.retrieveHotRootCommentsByPost(postId, pageable);
	}

	@GetMapping("/post/{postId}/new-count")
	@Operation(summary = "Count new root comments for a post", description = "Count approved top-level comments newer than the client's current anchor.")
	public ApiResponse<Long> countNewRootComments(
			@Parameter(description = "Post ID") @PathVariable Long postId,
			@Parameter(description = "Highest comment ID currently known by the client") @RequestParam(required = false) Long afterId)
	{
		return commentService.countNewRootCommentsByPost(postId, afterId);
	}

	@GetMapping("/post/{postId}/new")
	@Operation(summary = "Retrieve new root comments for a post", description = "Fetch approved top-level comments newer than the client's current anchor.")
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveNewRootComments(
			@Parameter(description = "Post ID") @PathVariable Long postId,
			@Parameter(description = "Highest comment ID currently known by the client") @RequestParam(required = false) Long afterId,
			@Parameter(description = "Number of comments to return") @RequestParam(defaultValue = "20") int size)
	{
		return commentService.retrieveNewRootCommentsByPost(postId, afterId, size);
	}

	@GetMapping("/{commentId}/context")
	@Operation(summary = "Locate a comment anchor", description = "Return the root comment and a reply window for positioning a highlighted comment.")
	public ApiResponse<CommentAnchorContextResponse> retrieveCommentAnchorContext(
			@Parameter(description = "Comment ID") @PathVariable Long commentId,
			@Parameter(description = "Number of replies to include around the root") @RequestParam(defaultValue = "20") int size)
	{
		return commentService.retrieveCommentAnchorContext(commentId, size);
	}

	@GetMapping("/{parentId}/replies")
	@Operation(summary = "Retrieve replies for a comment", description = "Fetch approved direct replies for a comment with pagination.")
	public ApiResponse<PageResult<CommentResponse>> retrieveReplies(
			@Parameter(description = "Parent comment ID") @PathVariable Long parentId,
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable)
	{
		return commentService.retrieveReplies(parentId, pageable);
	}

	@GetMapping("/{parentId}/replies/cursor")
	@Operation(summary = "Cursor-load replies for a comment", description = "Fetch approved direct replies using a stable cursor.")
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveRepliesCursor(
			@Parameter(description = "Parent comment ID") @PathVariable Long parentId,
			@Parameter(description = "Last seen reply ID from the previous response") @RequestParam(required = false) Long cursor,
			@Parameter(description = "Number of replies to return") @RequestParam(defaultValue = "20") int size)
	{
		return commentService.retrieveRepliesCursor(parentId, cursor, size);
	}

	@PostMapping("/{id}/report")
	@Operation(summary = "Report a comment", description = "Report an approved public comment for moderation review.")
	@PreAuthorize("isAuthenticated()")
	@RateLimit(count = 5, time = 30, unit = TimeUnit.MINUTES, message = "Too many reports. Please try again later.")
	public ApiResponse<Void> reportComment(@Parameter(description = "Comment ID") @PathVariable Long id,
			@Valid @RequestBody CommentReportRequest request)
	{
		return commentService.reportComment(id, request);
	}
}
