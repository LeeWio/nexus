package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@Schema(description = "Project showcase details with GitHub metrics")
public record ProjectResponse(
    @Schema(description = "Project ID")
    Long id,
    
    @Schema(description = "Project name")
    String name,
    
    @Schema(description = "Detailed description")
    String description,
    
    @Schema(description = "Cover image URL")
    String coverImage,
    
    @Schema(description = "GitHub repository URL")
    String githubUrl,
    
    @Schema(description = "Live preview URL")
    String previewUrl,
    
    @Schema(description = "Primary technology stack")
    String techStack,
    
    @Schema(description = "GitHub stars count")
    Integer starsCount,
    
    @Schema(description = "GitHub forks count")
    Integer forksCount,
    
    @Schema(description = "Primary programming language")
    String language,
    
    @Schema(description = "Manual display order")
    Integer sortOrder,
    
    @Schema(description = "Whether the project is published")
    Boolean isPublished,
    
    @Schema(description = "Creation time")
    LocalDateTime createdAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
