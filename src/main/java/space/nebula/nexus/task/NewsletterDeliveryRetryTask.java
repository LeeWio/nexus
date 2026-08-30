package space.nebula.nexus.task;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.NewsletterDeliveryService;

/**
 * Restores newsletter records that made it through persistence but not through
 * the asynchronous mail queue. The record contains the original template
 * snapshot, so a retry sends the same edition instead of regenerating it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsletterDeliveryRetryTask {
	private static final int RETRY_BATCH_SIZE = 100;
	private final NewsletterDeliveryService newsletterDeliveryService;

	@Scheduled(fixedDelay = 300_000)
	@SchedulerLock(name = "newsletterDeliveryRetry", lockAtMostFor = "PT4M")
	public void retryStaleDeliveries() {
		int retried = newsletterDeliveryService.retryStaleDeliveries(LocalDateTime.now().minusMinutes(10),
				RETRY_BATCH_SIZE);
		if (retried > 0)
			log.info("Requeued {} stale newsletter deliveries", retried);
	}
}
