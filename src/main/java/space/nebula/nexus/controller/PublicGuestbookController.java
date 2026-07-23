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
import space.nebula.nexus.payload.request.GuestbookRequest;
import space.nebula.nexus.payload.response.CommentResponse;
import space.nebula.nexus.payload.response.CursorPageResponse;
import space.nebula.nexus.payload.response.PageResult;
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

	@GetMapping("/roots")
	@Operation(summary = "Retrieve root guestbook entries", description = "Fetch approved top-level guestbook messages with pagination.")
	public ApiResponse<PageResult<CommentResponse>> retrieveRootComments(
			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return commentService.retrieveGuestbookRootComments(pageable);
	}

	@GetMapping("/roots/cursor")
	@Operation(summary = "Cursor-load root guestbook entries", description = "Fetch approved top-level guestbook messages using a stable cursor for infinite scrolling.")
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveRootCommentsCursor(
			@Parameter(description = "Last seen guestbook comment ID from the previous response") @RequestParam(required = false) Long cursor,
			@Parameter(description = "Number of guestbook comments to return") @RequestParam(defaultValue = "20") int size) {
		return commentService.retrieveGuestbookRootCommentsCursor(cursor, size);
	}

	@GetMapping("/roots/hot")
	@Operation(summary = "Retrieve hot guestbook entries", description = "Fetch approved top-level guestbook entries sorted by pinned, featured, likes, and recency.")
	public ApiResponse<PageResult<CommentResponse>> retrieveHotRootComments(
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 20) Pageable pageable) {
		return commentService.retrieveHotGuestbookRootComments(pageable);
	}

	@GetMapping("/new-count")
	@Operation(summary = "Count new root guestbook entries", description = "Count approved top-level guestbook messages newer than the client's current anchor.")
	public ApiResponse<Long> countNewRootComments(
			@Parameter(description = "Highest guestbook comment ID currently known by the client") @RequestParam(required = false) Long afterId) {
		return commentService.countNewGuestbookRootComments(afterId);
	}

	@GetMapping("/new")
	@Operation(summary = "Retrieve new guestbook entries", description = "Fetch approved top-level guestbook entries newer than the client's current anchor.")
	public ApiResponse<CursorPageResponse<CommentResponse>> retrieveNewRootComments(
			@Parameter(description = "Highest guestbook comment ID currently known by the client") @RequestParam(required = false) Long afterId,
			@Parameter(description = "Number of entries to return") @RequestParam(defaultValue = "20") int size) {
		return commentService.retrieveNewGuestbookRootComments(afterId, size);
	}

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	@Operation(summary = "Post to guestbook", description = "Submit a new message to the global guestbook. Requires authentication.")
	@RateLimit(count = 3, time = 10, unit = TimeUnit.MINUTES, message = "Too many guestbook posts. Please try again later.")
	public ApiResponse<Void> publishComment(@Valid @RequestBody GuestbookRequest request,
			HttpServletRequest servletRequest) {
		// Ensure postId is null for guestbook entries
		CommentRequest guestbookRequest = new CommentRequest(request.content(), null, request.parentId());
		return commentService.publishComment(guestbookRequest, servletRequest);
	}
}
