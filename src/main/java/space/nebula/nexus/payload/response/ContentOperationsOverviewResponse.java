package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Content-first overview for the administrative workspace.
 *
 * <p>This response intentionally stays compact so the dashboard can render one
 * coherent snapshot instead of orchestrating several independent requests.</p>
 */
@Schema(description = "Content operations overview for the administrative workspace")
public record ContentOperationsOverviewResponse(

		@Schema(description = "Content counts grouped by workflow state") Summary summary,

		@Schema(description = "Items that currently need editorial attention") List<AttentionItem> attentionItems,

		@Schema(description = "Most recently updated content items") List<QueueItem> editorialQueue,

		@Schema(description = "Recent content activity") List<ActivityItem> recentActivity,

		@Schema(description = "Snapshot generation time") LocalDateTime generatedAt) {

	public record Summary(long publishedPosts, long drafts, long pendingReview, long scheduled, long moments,
			long pendingComments, long unreadNotifications, long activeSubscribers) {
	}

	public record AttentionItem(String id, String type, String title, String description, String href,
			AttentionSeverity severity, long count) {
	}

	public record QueueItem(String id, String type, String title, String excerpt, PostStatus status,
			LocalDateTime updatedAt, String href) {
	}

	public record ActivityItem(String id, String type, String title, LocalDateTime occurredAt, String href) {
	}

	public enum AttentionSeverity {
		INFO, WARNING, CRITICAL
	}
}
