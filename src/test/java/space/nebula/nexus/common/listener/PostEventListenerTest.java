package space.nebula.nexus.common.listener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.service.IPostSearchService;
import space.nebula.nexus.service.IStaticGenerationService;
import space.nebula.nexus.utils.RedisUtil;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostEventListenerTest {
	@Mock
	private IPostSearchService postSearchService;

	@Mock
	private IStaticGenerationService staticGenerationService;

	@Mock
	private RedisUtil redisUtil;

	@InjectMocks
	private PostEventListener listener;

	@Test
	void removesPublicArtifactsWhenAnUpdatedPostLeavesPublication() {
		Post post = new Post();
		post.setId(17L);
		post.setSlug("former-public-post");
		post.setStatus(PostStatus.DRAFT);

		listener.onPostChanged(
				new PostChangedEvent(this, post, PostChangeType.UPDATED, "former-public-post", PostStatus.PUBLISHED));

		verify(redisUtil).delete(CacheConstants.POST_SLUG_PREFIX + "former-public-post");
		verify(postSearchService).deletePostIndex(17L);
		verify(staticGenerationService).deletePostStaticHtml("former-public-post");
		verify(postSearchService, never()).indexPost(post);
		verify(staticGenerationService, never()).generatePostStaticHtml(post);
	}
}
