package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.service.INotificationService;

/**
 * Creates follower notifications after an article publication transaction
 * commits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryPublicationNotificationListener {
	private final INotificationService notificationService;

	/**
	 * Notifies category followers only for the explicit publication transition.
	 *
	 * @param event
	 *            committed post change event
	 */
	@Async("asyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPostPublished(PostChangedEvent event) {
		if (event.getChangeType() != PostChangeType.PUBLISHED) {
			return;
		}
		int recipients = notificationService.sendCategoryPublication(event.getPost().getId());
		log.info("Processed category publication notifications for post {} with {} recipients", event.getPost().getId(),
				recipients);
	}
}
