package space.nebula.nexus.payload.response;

import lombok.Builder;
import space.nebula.nexus.enums.CommentStatus;

import java.time.LocalDateTime;

@Builder
public record CommentRiskResponse(Long id, Long parentId, Long postId, String postTitle, String content,
		String username, String nickname, String avatar, CommentStatus status, Long reportsCount, Long openReports,
		Long likesCount, long riskScore, LocalDateTime createdAt, LocalDateTime editedAt) {
}
