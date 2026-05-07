package space.nebula.nexus.payload.response;

import lombok.Builder;
import space.nebula.nexus.enums.PostStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Post response payload using Java 21 Record.
 */
@Builder
public record PostResponse(
    Long id,
    String title,
    String slug,
    String coverImage,
    String summary,
    String content,
    PostStatus status,
    Boolean isFeatured,
    Long views,
    String authorName,
    CategoryResponse category,
    Set<TagResponse> tags,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
