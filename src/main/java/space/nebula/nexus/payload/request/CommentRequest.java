package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting a comment on a post, moment, or (via guestbook
 * controller) the guestbook.
 */
@Schema(description = "Comment submission request. Provide postId XOR momentId; guestbook submissions omit both.")
public record CommentRequest(
		@Schema(description = "The markdown or plain text content of the comment", example = "Great post! Thanks for sharing.") @NotBlank(message = "Comment content is required") @Size(max = 1000, message = "Comment content must not exceed 1000 characters") String content,

		@Schema(description = "ID of the post being commented on", example = "1") Long postId,

		@Schema(description = "ID of the moment being commented on", example = "12") Long momentId,

		@Schema(description = "ID of the parent comment (for replies)", example = "0") Long parentId) {
}
