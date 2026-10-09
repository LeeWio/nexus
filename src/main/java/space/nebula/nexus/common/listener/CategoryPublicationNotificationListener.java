package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.service.INotificationService;

/**
 * Persists follower notifications in the article publication transaction.
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
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onPostPublished(PostChangedEvent event) {
		boolean publishedCreation = event.getChangeType() == PostChangeType.CREATED
				&& event.getPost().getStatus() == PostStatus.PUBLISHED;
		if (event.getChangeType() != PostChangeType.PUBLISHED && !publishedCreation) {
			return;
		}
		int recipients = notificationService.sendCategoryPublication(event.getPost().getId());
		log.info("Processed category publication notifications for post {} with {} recipients", event.getPost().getId(),
				recipients);
	}
}
