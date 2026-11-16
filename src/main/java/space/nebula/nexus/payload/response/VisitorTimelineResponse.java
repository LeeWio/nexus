package space.nebula.nexus.payload.response;

import java.time.Instant;

/** One page inside a visitor timeline. */
public record VisitorTimelineResponse(Instant viewedAt, String path, String title, String clientName, String deviceType,
		String sourceChannel, String referrerHost, String ipAddress) {
}
