package space.nebula.nexus.payload.response;

import java.io.Serializable;
import java.time.LocalDateTime;

public record TagResponse(
    Long id,
    String name,
    String slug,
    LocalDateTime createdAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
