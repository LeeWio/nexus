package space.nebula.nexus.payload.response;

import java.io.Serializable;
import java.time.LocalDateTime;

public record ProjectResponse(
    Long id,
    String name,
    String description,
    String coverImage,
    String githubUrl,
    String previewUrl,
    String techStack,
    Integer sortOrder,
    Boolean isPublished,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {
}
