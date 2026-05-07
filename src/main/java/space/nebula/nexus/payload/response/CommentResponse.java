package space.nebula.nexus.payload.response;

import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for displaying comments in a hierarchical tree.
 */
@Builder
public record CommentResponse(
    Long id,
    String content,
    String username,
    String avatar,
    LocalDateTime createdAt,
    List<CommentResponse> children
) {}
