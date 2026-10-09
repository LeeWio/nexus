package space.nebula.nexus.payload.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.NotificationCategory;
import java.util.Map;

@Schema(description = "Replaces explicit category overrides. Omitted categories inherit legacy settings; an empty overrides map resets all categories to inheritance.")
public record NotificationCategoryPreferenceRequest(
		@NotNull Map<@NotNull NotificationCategory, @NotNull @Valid Channels> overrides) {
	public record Channels(@NotNull Boolean inAppEnabled, @NotNull Boolean emailEnabled) {
	}
}
