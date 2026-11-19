package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.NotificationDeliveryRetentionProperties;
import space.nebula.nexus.repository.NotificationDeliveryRepository;
import space.nebula.nexus.repository.NotificationDeliveryRetryRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeliveryRetentionTask {
	private static final List<String> TERMINAL_STATUSES = List.of("DELIVERED", "FAILED", "ABANDONED");
	private final NotificationDeliveryRepository deliveries;
	private final NotificationDeliveryRetryRepository retries;
	private final NotificationDeliveryRetentionProperties properties;

	@Scheduled(cron = "0 30 2 * * ?")
	@SchedulerLock(name = "notificationDeliveryRetention", lockAtMostFor = "PT30M")
	@Transactional
	public void purgeExpiredRecords() {
		LocalDateTime now = LocalDateTime.now();
		long deliveryCount = purgeDeliveries(now.minusDays(properties.getDeliveryDays()));
		long retryCount = purgeRetries(now.minusDays(properties.getRetryAuditDays()));
		if (deliveryCount > 0 || retryCount > 0) {
			log.info("Purged expired notification delivery records. deliveries={}, retryAudits={}", deliveryCount,
					retryCount);
		}
	}

	private long purgeDeliveries(LocalDateTime cutoff) {
		long removed = 0;
		List<Long> ids;
		do {
			ids = deliveries.findRetentionIds(TERMINAL_STATUSES, cutoff, PageRequest.of(0, properties.getBatchSize()));
			if (!ids.isEmpty()) {
				deliveries.deleteAllByIdInBatch(ids);
				removed += ids.size();
			}
		} while (!ids.isEmpty());
		return removed;
	}

	private long purgeRetries(LocalDateTime cutoff) {
		long removed = 0;
		List<Long> ids;
		do {
			ids = retries.findRetentionIds(cutoff, PageRequest.of(0, properties.getBatchSize()));
			if (!ids.isEmpty()) {
				retries.deleteAllByIdInBatch(ids);
				removed += ids.size();
			}
		} while (!ids.isEmpty());
		return removed;
	}
}
