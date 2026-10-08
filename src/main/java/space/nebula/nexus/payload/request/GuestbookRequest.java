package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting a guestbook entry.
 */
@Schema(description = "Guestbook submission request")
public record GuestbookRequest(
		@Schema(description = "The markdown or plain text content of the entry", example = "Great site!") @NotBlank(message = "Guestbook content is required") @Size(max = 1000, message = "Guestbook content must not exceed 1000 characters") String content,

		@Schema(description = "ID of the parent comment (for replies)", example = "0") Long parentId,

		@Schema(description = "Display name for a guest entry. Required when the caller is not signed in.") @Size(max = 32, message = "Guest name must not exceed 32 characters") String guestName,

		@Schema(description = "Optional guest email. Stored for moderators and never shown publicly.") @Email(message = "Guest email must be a valid email address") @Size(max = 120, message = "Guest email must not exceed 120 characters") String guestEmail) {
}
