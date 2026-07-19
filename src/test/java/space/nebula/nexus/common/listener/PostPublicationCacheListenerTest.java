package space.nebula.nexus.common.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.entity.Post;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for publication-specific cache invalidation.
 */
@ExtendWith(MockitoExtension.class)
class PostPublicationCacheListenerTest
{
	@Mock
	private CacheManager cacheManager;

	@Mock
	private Cache blogCache;

	@Mock
	private Cache seoCache;

	@InjectMocks
	private PostPublicationCacheListener listener;

	@Test
	void clearsPublicCachesForPublishedPost()
	{
		when(cacheManager.getCache(CacheConstants.BLOG_POSTS)).thenReturn(blogCache);
		when(cacheManager.getCache(CacheConstants.SEO)).thenReturn(seoCache);

		listener.onVisibilityChanged(new PostChangedEvent(this, new Post(), PostChangeType.PUBLISHED));

		verify(blogCache).clear();
		verify(seoCache).clear();
	}

	@Test
	void clearsPublicCachesForArchivedPost()
	{
		when(cacheManager.getCache(CacheConstants.BLOG_POSTS)).thenReturn(blogCache);
		when(cacheManager.getCache(CacheConstants.SEO)).thenReturn(seoCache);

		listener.onVisibilityChanged(new PostChangedEvent(this, new Post(), PostChangeType.ARCHIVED));

		verify(blogCache).clear();
		verify(seoCache).clear();
	}

	@Test
	void ignoresNonVisibilityChanges()
	{
		listener.onVisibilityChanged(new PostChangedEvent(this, new Post(), PostChangeType.UPDATED));

		verify(cacheManager, never()).getCache(CacheConstants.BLOG_POSTS);
		verify(cacheManager, never()).getCache(CacheConstants.SEO);
	}
}
