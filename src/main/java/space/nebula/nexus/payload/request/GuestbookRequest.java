package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting a guestbook entry.
 */
@Schema(description = "Guestbook submission request")
public record GuestbookRequest(
		@Schema(description = "The markdown or plain text content of the entry", example = "Great site!") @NotBlank(message = "Guestbook content is required") @Size(max = 1000, message = "Guestbook content must not exceed 1000 characters") String content,

		@Schema(description = "ID of the parent comment (for replies)", example = "0") Long parentId) {
}
