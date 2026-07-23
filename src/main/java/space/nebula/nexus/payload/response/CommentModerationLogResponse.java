package space.nebula.nexus.payload.response;

import lombok.Builder;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentStatus;

import java.time.LocalDateTime;

@Builder
public record CommentModerationLogResponse(Long id, Long commentId, Long postId, String postTitle, Long parentId,
		String commentContent, CommentModerationAction action, CommentStatus previousStatus, CommentStatus newStatus,
		String moderatorUsername, String reason, String batchId, String note, LocalDateTime createdAt) {
}
