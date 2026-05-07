package space.nebula.nexus.payload.response;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CategoryResponse(
    Long id,
    String name,
    String slug,
    String description,
    LocalDateTime createdAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
