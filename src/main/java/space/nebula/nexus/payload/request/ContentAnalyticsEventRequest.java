package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import space.nebula.nexus.enums.ContentAnalyticsAction;

/** First-party browser event used for the anonymous post engagement funnel. */
@Schema(description = "Anonymous post engagement event")
public record ContentAnalyticsEventRequest(
		@NotNull @Schema(description = "Browser action", example = "READING_PROGRESS") ContentAnalyticsAction action,
		@NotNull @Schema(description = "Published post identifier", example = "42") Long postId,
		@Min(0) @Max(100) @Schema(description = "Current cumulative reading progress, required for READING_PROGRESS", example = "75") Integer progressPercent,
		@Min(0) @Max(86400) @Schema(description = "Cumulative active reading seconds, required for READING_PROGRESS", example = "142") Integer activeSeconds) {
}
