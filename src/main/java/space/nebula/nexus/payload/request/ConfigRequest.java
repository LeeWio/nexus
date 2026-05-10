package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ConfigRequest(
        @NotBlank(message = "Config key is required")
        @Size(max = 100, message = "Config key must not exceed 100 characters")
        String configKey,

        String configValue,

        @NotBlank(message = "Config name is required")
        @Size(max = 100, message = "Config name must not exceed 100 characters")
        String configName,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotNull(message = "isPublic flag is required")
        Boolean isPublic
) {}
