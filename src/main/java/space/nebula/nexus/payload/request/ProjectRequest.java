package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create or update a showcase project")
public record ProjectRequest(
    @Schema(description = "Name of the project", example = "Nexus CMS")
    @NotBlank(message = "Project name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    String name,

    @Schema(description = "Summary of the project goals and features")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    String description,

    @Schema(description = "URL to the project cover image")
    @Size(max = 500)
    String coverImage,

    @Schema(description = "GitHub repository link", example = "https://github.com/user/project")
    @Size(max = 255)
    String githubUrl,

    @Schema(description = "Live demo or preview link")
    @Size(max = 255)
    String previewUrl,

    @Schema(description = "Comma separated list of technologies used", example = "Java, Spring Boot, MySQL")
    @Size(max = 255)
    String techStack,

    @Schema(description = "Manual sorting priority", example = "0")
    Integer sortOrder,

    @Schema(description = "Whether the project is visible to visitors", example = "true")
    @NotNull(message = "Publish status is required")
    Boolean isPublished
) {}
