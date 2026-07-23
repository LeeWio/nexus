package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for removing a published post from public visibility.
 *
 * @param reason
 *            concise editorial reason for the archive action
 */
@Schema(description = "Post archive request")
public record PostArchiveRequest(
		@NotBlank(message = "An archive reason is required") @Size(max = 1000, message = "The archive reason must not exceed 1000 characters") @Schema(description = "Editorial archive reason", example = "Content requires substantial revision") String reason) {
}
