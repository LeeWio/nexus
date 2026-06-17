package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.PostContentType;

/**
 * Response for retrieving autosaved content.
 */
@Schema(description = "Autosaved post content and its format")
public record PostAutosaveResponse(
		@Schema(description = "In-progress content") Object content,

		@Schema(description = "Format of the content") PostContentType contentType) {
}
