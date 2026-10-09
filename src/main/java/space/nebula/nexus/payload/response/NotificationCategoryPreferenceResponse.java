package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.NotificationCategory;
import java.util.Map;

public record NotificationCategoryPreferenceResponse(Map<NotificationCategory, Channels> categories) {
	public record Channels(boolean inAppEnabled, boolean emailEnabled, boolean inherited) {
	}
}
