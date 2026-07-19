package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request for creating or renaming a personal post collection.
 *
 * @param name collection name unique within the current user's library
 * @param description optional collection description
 */
@Schema(description = "Personal post collection request")
public record PostCollectionRequest(
		@NotBlank @Size(max = 80) @Schema(description = "Collection name") String name,
		@Size(max = 300) @Schema(description = "Optional collection description") String description)
{
}
