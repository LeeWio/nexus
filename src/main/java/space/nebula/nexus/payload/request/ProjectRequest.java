package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    String coverImage,
    String githubUrl,
    String previewUrl,
    String techStack,
    Integer sortOrder,

    @NotNull(message = "Publish status is required")
    Boolean isPublished
) {}
