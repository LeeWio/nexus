package space.nebula.nexus.payload.response;

import java.io.Serializable;
import java.time.LocalDateTime;

public record MomentResponse(
    Long id,
    String content,
    Long likesCount,
    Boolean isPublished,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {
}
