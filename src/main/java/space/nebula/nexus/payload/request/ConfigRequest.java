package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "System configuration update/creation request")
public record ConfigRequest(
		@Schema(description = "Unique configuration key", example = "site_title") @NotBlank(message = "Config key is required") @Size(max = 100, message = "Config key must not exceed 100 characters") String configKey,

		@Schema(description = "Configuration value", example = "Nexus Blog") String configValue,

		@Schema(description = "Readable name for the configuration", example = "Site Title") @NotBlank(message = "Config name is required") @Size(max = 100, message = "Config name must not exceed 100 characters") String configName,

		@Schema(description = "Detailed explanation of this config", example = "The title shown on the home page and in browser tab") @Size(max = 255, message = "Description must not exceed 255 characters") String description,

		@Schema(description = "Whether this config is exposed to public API", example = "true") @NotNull(message = "isPublic flag is required") Boolean isPublic) {
}
