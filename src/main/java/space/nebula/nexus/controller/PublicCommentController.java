package space.nebula.nexus.controller;

import cn.hutool.core.lang.tree.Tree;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.CommentRequest;
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
	@Operation(summary = "Retrieve comments for a post", description = "Fetch a complete hierarchical tree of approved comments for a specific post.")
	public ApiResponse<List<Tree<Long>>> retrieveComments(
			@Parameter(description = "Post ID") @PathVariable Long postId) {
		return commentService.retrieveCommentsByPost(postId);
	}
}
