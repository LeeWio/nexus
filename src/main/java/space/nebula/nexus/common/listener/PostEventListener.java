package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostDeletedEvent;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.service.IPostRevisionService;
import space.nebula.nexus.service.IPostSearchService;

/**
 * Listener for Post related events. Decouples core business from side-effects
 * like indexing and revisioning.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

	private final IPostSearchService postSearchService;
	private final IPostRevisionService postRevisionService;

	/**
	 * Handle post created or updated. Executed AFTER transaction commit to ensure
	 * data integrity.
	 */
	@Async("asyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPostChanged(PostChangedEvent event) {
		log.info("Processing PostChangedEvent for post: {}", event.getPost().getId());

		// 1. Sync to Search Engine - ONLY if published
		if (event.getPost().getStatus() == PostStatus.PUBLISHED) {
			postSearchService.indexPost(event.getPost());
		} else {
			// If post was published but moved back to draft/archived, remove from index
			postSearchService.deletePostIndex(event.getPost().getId());
		}

		// 2. Save Revision history - Always save for any status change
		postRevisionService.saveRevision(event.getPost());
	}

	/**
	 * Handle post deletion.
	 */
	@Async("asyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPostDeleted(PostDeletedEvent event) {
		log.info("Processing PostDeletedEvent for post: {}", event.getPostId());

		// Remove from Search Engine
		postSearchService.deletePostIndex(event.getPostId());
	}
}
