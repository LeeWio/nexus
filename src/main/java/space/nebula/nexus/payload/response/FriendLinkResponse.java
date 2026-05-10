package space.nebula.nexus.payload.response;

import java.io.Serializable;
import java.time.LocalDateTime;

public record FriendLinkResponse(
    Long id,
    String name,
    String url,
    String avatar,
    String description,
    Integer sortOrder,
    Boolean isPublished,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) implements Serializable {
}
