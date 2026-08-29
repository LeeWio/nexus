package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.NotificationDeliveryService;

import java.time.LocalDateTime;

/**
 * Recovers notification deliveries that could not be handed to, or completed
 * by, the asynchronous mail queue. RabbitMQ handles short retries; this task
 * restores durable delivery records after that boundary.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeliveryRetryTask {
	private static final int RETRY_BATCH_SIZE = 100;
	private final NotificationDeliveryService notificationDeliveryService;

	@Scheduled(fixedDelay = 300_000)
	@SchedulerLock(name = "notificationDeliveryRetry", lockAtMostFor = "PT4M")
	public void retryStaleDeliveries() {
		int retried = notificationDeliveryService.retryStaleEmailDeliveries(LocalDateTime.now().minusMinutes(10),
				RETRY_BATCH_SIZE);
		if (retried > 0) log.info("Requeued {} stale notification email deliveries", retried);
	}
}
