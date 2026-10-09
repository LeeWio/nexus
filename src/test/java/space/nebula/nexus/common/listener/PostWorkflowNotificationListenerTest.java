package space.nebula.nexus.common.listener;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.NotificationType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.payload.response.NotificationContext;
import space.nebula.nexus.service.INotificationService;

import static org.mockito.Mockito.*;
import static space.nebula.nexus.payload.response.NotificationContext.Action.EDIT;
import static space.nebula.nexus.payload.response.NotificationContext.Action.REVIEW;
import static space.nebula.nexus.payload.response.NotificationContext.Action.VIEW;
import static space.nebula.nexus.payload.response.NotificationContext.ObjectType.POST;

class PostWorkflowNotificationListenerTest {
	@Test
	void reviewSubmissionExcludesAuthorFromAdministratorAudience() {
		INotificationService notifications = mock(INotificationService.class);
		var event = new PostChangedEvent(this, post(), PostChangeType.SUBMITTED_FOR_REVIEW);
		new PostWorkflowNotificationListener(notifications).onPostChanged(event);
		verify(notifications).sendToPostReviewers("Post awaiting review", "\"Example\" is ready for review.",
				"/posts?id=7", event.getNotificationEventId(), 2L, new NotificationContext(POST, 7L, 2L, REVIEW));
	}

	@Test
	void publicationNotifiesAuthorIncludingScheduledPublication() {
		INotificationService notifications = mock(INotificationService.class);
		Post post = post();
		var event = new PostChangedEvent(this, post, PostChangeType.PUBLISHED);
		new PostWorkflowNotificationListener(notifications).onPostChanged(event);
		verify(notifications).sendOnce(post.getAuthor(), "Post published", "\"Example\" is now published.",
				NotificationType.POST_PUBLISHED, "/post/example", event.getNotificationEventId(),
				new NotificationContext(POST, 7L, event.getNotificationActorId(), VIEW));
	}

	@Test
	void rejectedPostIncludesReviewReasonOnlyForAuthor() {
		INotificationService notifications = mock(INotificationService.class);
		Post post = post();
		post.setReviewComment("Please cite sources.");
		var event = new PostChangedEvent(this, post, PostChangeType.REJECTED);
		new PostWorkflowNotificationListener(notifications).onPostChanged(event);
		verify(notifications).sendOnce(post.getAuthor(), "Post changes requested",
				"\"Example\" was not approved. Please cite sources.", NotificationType.POST_REJECTED, "/posts?id=7",
				event.getNotificationEventId(),
				new NotificationContext(POST, 7L, event.getNotificationActorId(), EDIT));
		verifyNoMoreInteractions(notifications);
	}

	@Test
	void ordinaryEditsDoNotNotify() {
		INotificationService notifications = mock(INotificationService.class);
		new PostWorkflowNotificationListener(notifications)
				.onPostChanged(new PostChangedEvent(this, post(), PostChangeType.UPDATED));
		verifyNoInteractions(notifications);
	}

	private Post post() {
		User author = new User();
		author.setId(2L);
		Post post = new Post();
		post.setId(7L);
		post.setTitle("Example");
		post.setSlug("example");
		post.setAuthor(author);
		return post;
	}

	@Test
	void directlyCreatedPublishedArticleProducesPublicationReceipt() {
		INotificationService notifications = mock(INotificationService.class);
		Post post = post();
		post.setStatus(PostStatus.PUBLISHED);
		var event = new PostChangedEvent(this, post, PostChangeType.CREATED);
		new PostWorkflowNotificationListener(notifications).onPostChanged(event);
		verify(notifications).sendOnce(post.getAuthor(), "Post published", "\"Example\" is now published.",
				NotificationType.POST_PUBLISHED, "/post/example", event.getNotificationEventId(),
				new NotificationContext(POST, 7L, event.getNotificationActorId(), VIEW));
	}

	@Test
	void draftCreationDoesNotProducePublicationReceipt() {
		INotificationService notifications = mock(INotificationService.class);
		Post post = post();
		post.setStatus(PostStatus.DRAFT);
		new PostWorkflowNotificationListener(notifications)
				.onPostChanged(new PostChangedEvent(this, post, PostChangeType.CREATED));
		verifyNoInteractions(notifications);
	}
}
