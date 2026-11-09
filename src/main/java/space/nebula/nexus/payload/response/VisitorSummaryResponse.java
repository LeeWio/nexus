package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * One visitor in the admin list. The IP is visible only on this admin surface.
 */
@Schema(description = "Anonymous visitor summarized over the selected window")
public record VisitorSummaryResponse(
		@Schema(description = "Stable visitor key. Use it to load the timeline.") String visitorHash,
		@Schema(description = "Most recent client IP in the window") String ipAddress, String country, String region,
		String city, String clientName, String osName, String deviceType, LocalDateTime firstSeen,
		LocalDateTime lastSeen, long views, long sessions, long pages) {
}
