package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import space.nebula.nexus.enums.PostContentType;

@Schema(description = "Request body for autosaving post content")
public record PostAutosaveRequest(
		@Schema(description = "Post ID (for existing posts) or a client-generated UUID (for new posts)", example = "1")
		@NotBlank(message = "Identifier is required for autosave")
		@Pattern(regexp = "(?:[1-9][0-9]{0,18}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12})",
				message = "Identifier must be a positive post ID or UUID") String identifier,

		@Schema(description = "In-progress content being saved") @NotNull(message = "Content cannot be null") Object content,

		@Schema(description = "Format of the content (JSON, MDX)") PostContentType contentType) {
}
