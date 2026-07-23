package space.nebula.nexus.payload.response;

import lombok.Builder;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;

import java.time.LocalDateTime;

@Builder
public record CommentReportResponse(Long commentId, Long reporterId, String reporterUsername,
		String reporterNickname, String reason, String description, CommentReportStatus status, String handledBy,
		LocalDateTime handledAt, String resolutionNote, Long postId, String postTitle, Long parentId,
		CommentStatus commentStatus, String commentContent, LocalDateTime createdAt) {
}
