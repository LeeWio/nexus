package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.service.IInteractionService;

/**
 * Controller for handling public user social interactions. Provides mechanism
 * for liking and favoriting blog content.
 */
@Tag(name = "User Interactions", description = "Endpoints for social interactions including likes and favorites")
@RestController
@RequestMapping("/api/v1/public/interactions")
@RequiredArgsConstructor
public class PublicInteractionController
{

	private final IInteractionService interactionService;

	@PostMapping("/posts/{postId}/like")
	@Operation(summary = "Like a post", description = "Add a like to a specific blog post. Requires user authentication.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Void> likePost(@Parameter(description = "Post ID") @PathVariable Long postId)
	{
		return interactionService.likePost(postId);
	}

	@PostMapping("/posts/{postId}/unlike")
	@Operation(summary = "Unlike a post", description = "Remove a previously added like from a blog post.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Void> unlikePost(@Parameter(description = "Post ID") @PathVariable Long postId)
	{
		return interactionService.unlikePost(postId);
	}

	@PostMapping("/comments/{commentId}/like")
	@Operation(summary = "Like a comment", description = "Add a like to an approved comment. Requires user authentication.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Void> likeComment(@Parameter(description = "Comment ID") @PathVariable Long commentId)
	{
		return interactionService.likeComment(commentId);
	}

	@PostMapping("/comments/{commentId}/unlike")
	@Operation(summary = "Unlike a comment", description = "Remove a previously added like from a comment.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Void> unlikeComment(@Parameter(description = "Comment ID") @PathVariable Long commentId)
	{
		return interactionService.unlikeComment(commentId);
	}

	@PostMapping("/posts/{postId}/favorite")
	@Operation(summary = "Favorite a post", description = "Bookmark a post as a user favorite. Requires user authentication.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Void> favoritePost(@Parameter(description = "Post ID") @PathVariable Long postId)
	{
		return interactionService.favoritePost(postId);
	}

	@PostMapping("/posts/{postId}/unfavorite")
	@Operation(summary = "Unfavorite a post", description = "Remove a post from the user's bookmarks.")
	@PreAuthorize("isAuthenticated()")
	public ApiResponse<Void> unfavoritePost(@Parameter(description = "Post ID") @PathVariable Long postId)
	{
		return interactionService.unfavoritePost(postId);
	}
}
