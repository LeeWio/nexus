package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Safe API representation of a user notification without recipient account
 * data.
 */
@Schema(description = "One in-app notification owned by the current user")
public record NotificationResponse(@Schema(description = "Notification ID") Long id,
		@Schema(description = "Short notification heading") String title,
		@Schema(description = "Notification body") String content,
		@Schema(description = "Stable notification type, such as COMMENT_REPLY, POST_COMMENT, CATEGORY_POST, or HEALTH_CHECK") String type,
		@Schema(description = "Whether the notification has been read") Boolean read,
		@Schema(description = "Whether the notification is saved for later review") Boolean saved,
		@Schema(description = "Time at which the notification was marked read; null when unread") LocalDateTime readAt,
		@Schema(description = "Time at which the notification was completed; null while it remains in the inbox") LocalDateTime completedAt,
		@Schema(description = "Optional frontend-relative route associated with this notification") String link,
		@Schema(description = "Creation time") LocalDateTime createdAt) {
}
