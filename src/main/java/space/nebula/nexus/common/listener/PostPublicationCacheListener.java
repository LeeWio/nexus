package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.enums.PostStatus;

/**
 * Invalidates public post and SEO caches after a visibility transition commits.
 */
@Component
@RequiredArgsConstructor
public class PostPublicationCacheListener
{
	private final CacheManager cacheManager;

	/**
	 * Clears collection-level caches only for a committed visibility change. Running
	 * after commit avoids exposing uncommitted post state and avoids clearing the
	 * caches every time the scheduler finds no work.
	 *
	 * @param event committed post change event
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onVisibilityChanged(PostChangedEvent event)
	{
		if (!requiresPublicCacheInvalidation(event))
		{
			return;
		}
		clear(CacheConstants.BLOG_POSTS);
		clear(CacheConstants.SEO);
	}

	private boolean requiresPublicCacheInvalidation(PostChangedEvent event)
	{
		if (event.getChangeType() == PostChangeType.PUBLISHED || event.getChangeType() == PostChangeType.ARCHIVED
				|| event.getChangeType() == PostChangeType.RESTORED_TO_DRAFT
				|| event.getChangeType() == PostChangeType.SCHEDULE_CANCELED)
		{
			return true;
		}
		return event.getChangeType() == PostChangeType.UPDATED && event.getPost().getStatus() == PostStatus.PUBLISHED;
	}

	private void clear(String cacheName)
	{
		Cache cache = cacheManager.getCache(cacheName);
		if (cache != null)
		{
			cache.clear();
		}
	}
}
