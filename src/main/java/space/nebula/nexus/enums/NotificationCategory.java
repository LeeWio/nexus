package space.nebula.nexus.enums;

/**
 * Fine-grained groups; missing overrides inherit the corresponding legacy
 * channel setting.
 */
public enum NotificationCategory {
	COMMENT, CATEGORY_POST, CREATOR, MODERATION, REPORT, OPERATIONS;

	public static NotificationCategory forType(String type) {
		return switch (type) {
			case "COMMENT_APPROVED", "COMMENT_REJECTED", "COMMENT_REPLY", "POST_COMMENT", "MOMENT_COMMENT" -> COMMENT;
			case "CATEGORY_POST" -> CATEGORY_POST;
			case "POST_REJECTED", "POST_PUBLISHED", "POST_SCHEDULED", "POST_SCHEDULE_CANCELED", "POST_ARCHIVED" ->
				CREATOR;
			case "COMMENT_PENDING_REVIEW", "COMMENT_FLAGGED", "POST_PENDING_REVIEW", "FRIEND_LINK_APPLICATION",
					"GUESTBOOK_COMMENT", "USER_PENDING_REVIEW" ->
				MODERATION;
			case "COMMENT_REPORT_RECEIVED", "COMMENT_REPORT_RESOLVED", "POST_REPORT_RECEIVED", "POST_REPORT_RESOLVED" ->
				REPORT;
			default -> OPERATIONS;
		};
	}
}
