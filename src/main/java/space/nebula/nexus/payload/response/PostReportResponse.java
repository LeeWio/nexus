package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.PostReportStatus;
import space.nebula.nexus.enums.PostStatus;

import java.time.LocalDateTime;

/**
 * Administrative representation of a reader report against an article.
 */
public record PostReportResponse(Long postId, String postTitle, String postSlug, PostStatus postStatus,
		Long reporterId, String reporterUsername, String reporterNickname, String reason, String description,
		PostReportStatus status, String handledBy, LocalDateTime handledAt, String resolutionNote,
		LocalDateTime createdAt) {
}
