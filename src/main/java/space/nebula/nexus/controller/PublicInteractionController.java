package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.CommentInteractionResponse;
import space.nebula.nexus.payload.response.PostInteractionResponse;
import space.nebula.nexus.service.IInteractionService;

/**
 * Controller for handling public user social interactions. Provides mechanism
 * for liking and favoriting blog content.
 */
@Tag(name = "User Interactions", description = "Endpoints for social interactions including likes and favorites")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/public/interactions")
@RequiredArgsConstructor
public class PublicInteractionController {

	private final IInteractionService interactionService;

	@PostMapping("/posts/{postId}/like")
	@Operation(summary = "Like a post", description = "Add a like to a specific published post. The response returns the caller's final like and favorite state plus the current counters, so optimistic UI state can be reconciled without refetching the post.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PostInteractionResponse> likePost(
			@Parameter(description = "Published post ID") @PathVariable Long postId) {
		return interactionService.likePost(postId);
	}

	@PostMapping("/posts/{postId}/unlike")
	@Operation(summary = "Unlike a post", description = "Remove the current user's like from a post. This command is idempotent; the response always contains the final interaction state and current counters.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PostInteractionResponse> unlikePost(
			@Parameter(description = "Post ID") @PathVariable Long postId) {
		return interactionService.unlikePost(postId);
	}

	@PostMapping("/comments/{commentId}/like")
	@Operation(summary = "Like a comment", description = "Add a like to an approved comment. The response returns the caller's final like state and current counter for optimistic UI reconciliation.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<CommentInteractionResponse> likeComment(
			@Parameter(description = "Approved comment ID") @PathVariable Long commentId) {
		return interactionService.likeComment(commentId);
	}

	@PostMapping("/comments/{commentId}/unlike")
	@Operation(summary = "Unlike a comment", description = "Remove the current user's like from a comment. This command is idempotent; the response always contains the final like state and current counter.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<CommentInteractionResponse> unlikeComment(
			@Parameter(description = "Comment ID") @PathVariable Long commentId) {
		return interactionService.unlikeComment(commentId);
	}

	@PostMapping("/posts/{postId}/favorite")
	@Operation(summary = "Favorite a post", description = "Bookmark a published post. The response returns the caller's final like and favorite state plus current counters, so no follow-up post request is required.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PostInteractionResponse> favoritePost(
			@Parameter(description = "Published post ID") @PathVariable Long postId) {
		return interactionService.favoritePost(postId);
	}

	@PostMapping("/posts/{postId}/unfavorite")
	@Operation(summary = "Unfavorite a post", description = "Remove a post from the current user's bookmarks. This command is idempotent; the response always contains the final interaction state and current counters.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<PostInteractionResponse> unfavoritePost(
			@Parameter(description = "Post ID") @PathVariable Long postId) {
		return interactionService.unfavoritePost(postId);
	}
}
