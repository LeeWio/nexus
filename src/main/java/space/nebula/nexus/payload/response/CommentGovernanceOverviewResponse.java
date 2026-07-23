package space.nebula.nexus.payload.response;

import lombok.Builder;
import space.nebula.nexus.enums.CommentModerationAction;
import space.nebula.nexus.enums.CommentReportStatus;
import space.nebula.nexus.enums.CommentStatus;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CommentGovernanceOverviewResponse(long totalComments, long pendingComments, long approvedComments,
		long rejectedComments, long spamComments, long openReports, long actionedReports, long dismissedReports,
		long reportsLast24Hours, long autoFlaggedLast24Hours, LocalDateTime oldestPendingAt,
		List<CommentStatusCount> commentsByStatus, List<ReportStatusCount> reportsByStatus,
		List<ModerationActionCount> moderationActionsLast7Days) {

	@Builder
	public record CommentStatusCount(CommentStatus status, long count) {
	}

	@Builder
	public record ReportStatusCount(CommentReportStatus status, long count) {
	}

	@Builder
	public record ModerationActionCount(CommentModerationAction action, long count) {
	}
}
