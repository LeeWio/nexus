package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Complete replacement of the current user's in-app notification settings.
 */
@Schema(description = "In-app notification delivery preferences")
public record NotificationPreferenceRequest(
		@Schema(description = "Allow comment, reply, and moderation notifications", example = "true") @NotNull(message = "Comment notification preference is required") Boolean commentNotificationsEnabled,
		@Schema(description = "Allow notifications for posts in followed categories", example = "true") @NotNull(message = "Category post notification preference is required") Boolean categoryPostNotificationsEnabled,
		@Schema(description = "Allow system and operational notifications", example = "true") @NotNull(message = "System notification preference is required") Boolean systemNotificationsEnabled,
		@Schema(description = "Also deliver comment activity by email. Defaults to false for legacy clients.", example = "false") Boolean commentEmailNotificationsEnabled,
		@Schema(description = "Also deliver followed-category posts by email. Defaults to false for legacy clients.", example = "false") Boolean categoryPostEmailNotificationsEnabled,
		@Schema(description = "Also deliver system and operational activity by email. Defaults to false for legacy clients.", example = "false") Boolean systemEmailNotificationsEnabled) {
}
