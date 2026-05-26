package space.nebula.nexus.controller;

import cn.hutool.core.lang.tree.Tree;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.payload.request.CommentRequest;
import space.nebula.nexus.payload.request.GuestbookRequest;
import space.nebula.nexus.service.ICommentService;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the public guestbook. Provides a global message board for user
 * interactions and feedback.
 */
@Tag(name = "Public Guestbook", description = "Public endpoints for the global site guestbook")
@RestController
@RequestMapping("/api/v1/public/guestbook")
@RequiredArgsConstructor
public class PublicGuestbookController {

	private final ICommentService commentService;

	@GetMapping
	@Operation(summary = "Retrieve guestbook entries", description = "Fetch a complete tree of approved guestbook messages.")
	public ApiResponse<List<Tree<Long>>> retrieveComments() {
		return commentService.retrieveGuestbookComments();
	}

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Post to guestbook", description = "Submit a new message to the global guestbook. Requires authentication.")
	@RateLimit(count = 3, time = 10, unit = TimeUnit.MINUTES, message = "Guestbook posting frequency too high. Please try again later.")
	public ApiResponse<Void> publishComment(@Valid @RequestBody GuestbookRequest request,
			HttpServletRequest servletRequest) {
		// Ensure postId is null for guestbook entries
		CommentRequest guestbookRequest = new CommentRequest(request.content(), null, request.parentId());
		return commentService.publishComment(guestbookRequest, servletRequest);
	}
}
