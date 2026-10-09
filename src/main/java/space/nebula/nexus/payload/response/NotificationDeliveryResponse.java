package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.NotificationDeliveryStatus;
import java.time.LocalDateTime;

/**
 * Operational metadata only; no email body, recipient address, or event key is
 * exposed.
 */
public record NotificationDeliveryResponse(Long id, Long notificationId, String channel,
		NotificationDeliveryStatus status, int attempts, int maxAttempts, String lastError, LocalDateTime nextAttemptAt,
		LocalDateTime deliveredAt, LocalDateTime createdAt, boolean retryable, NotificationContext context) {
}
