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
		PurgeCounts deliveryPurge = purgeDeliveries(now.minusDays(properties.getDeliveryDays()));
		long deliveryCount = deliveryPurge.deliveries();
		long retryCount = deliveryPurge.retries() + purgeRetries(now.minusDays(properties.getRetryAuditDays()));
		if (deliveryCount > 0 || retryCount > 0) {
			log.info("Purged expired notification delivery records. deliveries={}, retryAudits={}", deliveryCount,
					retryCount);
		}
	}

	private PurgeCounts purgeDeliveries(LocalDateTime cutoff) {
		long deliveriesRemoved = 0;
		long retriesRemoved = 0;
		List<Long> ids;
		do {
			ids = deliveries.findRetentionIds(TERMINAL_STATUSES, cutoff, PageRequest.of(0, properties.getBatchSize()));
			if (!ids.isEmpty()) {
				retriesRemoved += retries.deleteByDeliveryIdIn(ids);
				deliveries.deleteAllByIdInBatch(ids);
				deliveriesRemoved += ids.size();
			}
		} while (!ids.isEmpty());
		return new PurgeCounts(deliveriesRemoved, retriesRemoved);
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

	private record PurgeCounts(long deliveries, long retries) {
	}
}
