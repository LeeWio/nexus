package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.ICommentService;

import java.util.concurrent.TimeUnit;

/**
 * Controller for public comment operations. Allows readers to view and submit
 * comments on blog posts.
 */
@Tag(name = "Public Comment API", description = "Public endpoints for viewing and submitting user comments")
@RestController
@RequestMapping("/api/v1/public/comments")
@RequiredArgsConstructor
public class PublicCommentController {

	private final ICommentService commentService;

	@PostMapping
	@Operation(summary = "Publish a new comment", description = "Submit a comment on a blog post. Requires user authentication.")
	@RateLimit(count = 5, time = 15, unit = TimeUnit.MINUTES, message = "Comment frequency too high. Please wait a moment.")
	public ApiResponse<Void> publishComment(@Valid @RequestBody CommentRequest request,
			HttpServletRequest servletRequest) {
		return commentService.publishComment(request, servletRequest);
	}

	@GetMapping("/post/{postId}")
	@Operation(summary = "Retrieve comments for a post", description = "Fetch a paginated, hierarchical list of approved comments for a specific post.")
	public ApiResponse<PageResult<CommentResponse>> retrieveComments(
			@Parameter(description = "Post ID") @PathVariable Long postId,
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
		return commentService.retrieveCommentsByPost(postId, pageable);
	}
}
