package space.nebula.nexus.payload.response;

import java.time.LocalDateTime;

/** One page inside a visitor timeline. */
public record VisitorTimelineResponse(LocalDateTime viewedAt, String path, String title, String clientName,
		String deviceType, String sourceChannel, String referrerHost, String ipAddress) {
}
