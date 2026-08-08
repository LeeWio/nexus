package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Authoritative interaction state for one post after a like or favorite command.
 */
@Schema(description = "The authenticated user's final interaction state and current counters for a post")
public record PostInteractionResponse(@Schema(description = "Post ID") Long postId,

		@Schema(description = "Whether the authenticated user currently likes the post") boolean liked,

		@Schema(description = "Whether the authenticated user currently has the post in favorites") boolean favorited,

		@Schema(description = "Current total number of post likes", example = "42") long likesCount,

		@Schema(description = "Current total number of post favorites", example = "12") long favoritesCount)
		implements Serializable {
	private static final long serialVersionUID = 1L;
}
