package space.nebula.nexus.payload.response;

import java.time.LocalDateTime;

/**
 * Safe delivery failure detail that deliberately omits subscriber email and
 * tokens.
 */
public record NewsletterDeliveryResponse(Long id, Long subscriberId, String status, Integer attempts, String lastError,
		LocalDateTime deliveredAt, LocalDateTime createdAt) {
}
