package space.nebula.nexus.payload.response;

import java.time.LocalDateTime;

public record NotificationDeliveryRetryResponse(Long id, String requestId, String requestedBy, String reason,
		String previousStatus, int previousMaxAttempts, int newMaxAttempts, int attemptsAtRetry,
		LocalDateTime createdAt) {
}
