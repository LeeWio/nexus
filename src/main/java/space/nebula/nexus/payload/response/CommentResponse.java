package space.nebula.nexus.payload.response;

import lombok.Builder;
import space.nebula.nexus.enums.CommentStatus;
import java.time.LocalDateTime;

/**
 * Response DTO for displaying comments. Kept flat as TreeUtil handles the hierarchy.
 */
@Builder
public record CommentResponse(Long id, Long parentId, String content, String username, String nickname, String avatar,
		CommentStatus status, Long postId, String postTitle, LocalDateTime createdAt) {
}
