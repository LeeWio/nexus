package space.nebula.nexus.common.listener;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.service.INotificationService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryPublicationNotificationListenerTest {
	@Test
	void explicitPublicationNotifiesFollowers() {
		INotificationService notificationService = mock(INotificationService.class);
		CategoryPublicationNotificationListener listener = new CategoryPublicationNotificationListener(
				notificationService);
		Post post = post(7L);
		when(notificationService.sendCategoryPublication(7L)).thenReturn(12);

		listener.onPostPublished(new PostChangedEvent(this, post, PostChangeType.PUBLISHED));

		verify(notificationService).sendCategoryPublication(7L);
	}

	@Test
	void ordinaryUpdateDoesNotNotifyFollowers() {
		INotificationService notificationService = mock(INotificationService.class);
		CategoryPublicationNotificationListener listener = new CategoryPublicationNotificationListener(
				notificationService);

		listener.onPostPublished(new PostChangedEvent(this, post(7L), PostChangeType.UPDATED));

		verify(notificationService, never()).sendCategoryPublication(7L);
	}

	private Post post(Long id) {
		Post post = new Post();
		post.setId(id);
		post.setSlug("post-" + id);
		return post;
	}
}
