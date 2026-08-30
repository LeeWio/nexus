package space.nebula.nexus.payload.response;

import java.time.LocalDateTime;

/** Read-only delivery metrics for one newsletter broadcast. */
public record NewsletterDeliveryBatchResponse(Long id, String status, Integer recipientCount, Integer queuedCount,
		Integer deliveredCount, Integer failedCount, LocalDateTime startedAt, LocalDateTime completedAt) {
}
