package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Current in-app notification delivery settings for an authenticated user.
 */
@Schema(description = "In-app notification delivery preferences")
public record NotificationPreferenceResponse(
		@Schema(description = "Whether comment, reply, and moderation notifications are delivered") boolean commentNotificationsEnabled,
		@Schema(description = "Whether posts in followed categories create notifications") boolean categoryPostNotificationsEnabled,
		@Schema(description = "Whether system and operational notifications are delivered") boolean systemNotificationsEnabled,
		@Schema(description = "Whether comment, reply, and moderation notifications are also delivered by email") boolean commentEmailNotificationsEnabled,
		@Schema(description = "Whether followed-category posts are also delivered by email") boolean categoryPostEmailNotificationsEnabled,
		@Schema(description = "Whether system and operational notifications are also delivered by email") boolean systemEmailNotificationsEnabled) {
}
