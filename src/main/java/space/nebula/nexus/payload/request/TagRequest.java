package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Tag creation/update request")
public record TagRequest(
    @Schema(description = "Display name of the tag", example = "Java")
    @NotBlank(message = "Name cannot be empty")
    @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters")
    String name,

    @Schema(description = "URL friendly slug", example = "java")
    @NotBlank(message = "Slug cannot be empty")
    @Size(min = 1, max = 50, message = "Slug must be between 1 and 50 characters")
    String slug
) {}
