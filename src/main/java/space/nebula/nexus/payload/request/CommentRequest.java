package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting a comment.
 */
@Schema(description = "Comment submission request")
public record CommentRequest(
		@Schema(description = "The markdown or plain text content of the comment", example = "Great post! Thanks for sharing.") @NotBlank(message = "Comment content cannot be empty") @Size(max = 1000, message = "Comment content too long (max 1000 characters)") String content,

		@Schema(description = "ID of the post being commented on", example = "1") @NotNull(message = "Post ID is required") Long postId,

		@Schema(description = "ID of the parent comment (for replies)", example = "0") Long parentId) {
}
