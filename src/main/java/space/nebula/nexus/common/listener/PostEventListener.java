package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostDeletedEvent;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.service.IStaticGenerationService;
import space.nebula.nexus.service.IPostSearchService;
import space.nebula.nexus.utils.RedisUtil;
import space.nebula.nexus.common.constant.CacheConstants;

/**
 * Listener for post change side effects that run after transaction commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostEventListener {

	private final IPostSearchService postSearchService;
	private final IStaticGenerationService staticGenerationService;
	private final RedisUtil redisUtil;

	/**
	 * Handle post created or updated. Executed AFTER transaction commit to ensure
	 * data integrity.
	 */
	@Async("asyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPostChanged(PostChangedEvent event) {
		log.info("Processing PostChangedEvent for post: {}", event.getPost().getId());
		redisUtil.delete(CacheConstants.POST_SLUG_PREFIX + event.getPost().getSlug());
		if (event.getPreviousSlug() != null && !event.getPost().getSlug().equals(event.getPreviousSlug())) {
			redisUtil.delete(CacheConstants.POST_SLUG_PREFIX + event.getPreviousSlug());
		}

		// 1. Sync to Search Engine - ONLY if published
		if (event.getPost().getStatus() == PostStatus.PUBLISHED) {
			postSearchService.indexPost(event.getPost());
			staticGenerationService.generatePostStaticHtml(event.getPost());
		} else {
			// If post was published but moved back to draft/archived, remove from index
			postSearchService.deletePostIndex(event.getPost().getId());
			if (event.getChangeType() == PostChangeType.ARCHIVED
					|| event.getChangeType() == PostChangeType.RESTORED_TO_DRAFT) {
				staticGenerationService.deletePostStaticHtml(event.getPost().getSlug());
			}
		}
		if (event.getPreviousSlug() != null && !event.getPost().getSlug().equals(event.getPreviousSlug())) {
			staticGenerationService.deletePostStaticHtml(event.getPreviousSlug());
		}

	}

	/**
	 * Handle post deletion.
	 */
	@Async("asyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onPostDeleted(PostDeletedEvent event) {
		log.info("Processing PostDeletedEvent for post: {}", event.getPostId());
		redisUtil.delete(CacheConstants.POST_SLUG_PREFIX + event.getSlug());

		// Remove from Search Engine
		postSearchService.deletePostIndex(event.getPostId());
		staticGenerationService.deletePostStaticHtml(event.getSlug());
	}
}
