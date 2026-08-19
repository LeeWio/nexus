package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Authoritative interaction state for one comment after a like command.
 */
@Schema(description = "The authenticated user's final like state and current counter for a comment")
public record CommentInteractionResponse(@Schema(description = "Comment ID") Long commentId,

		@Schema(description = "Whether the authenticated user currently likes the comment") boolean liked,

		@Schema(description = "Current total number of comment likes", example = "7") long likesCount)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;
}
