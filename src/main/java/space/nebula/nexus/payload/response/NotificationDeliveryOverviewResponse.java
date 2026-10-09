package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.NotificationDeliveryStatus;
import java.time.LocalDateTime;
import java.util.Map;

public record NotificationDeliveryOverviewResponse(Map<NotificationDeliveryStatus, Long> counts, long pending,
		long overdue, LocalDateTime oldestPendingAt, LocalDateTime observedAt) {
}
