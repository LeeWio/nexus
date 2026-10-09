package space.nebula.nexus.common.listener;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.NotificationType;
import space.nebula.nexus.payload.response.NotificationContext;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.INotificationService;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static space.nebula.nexus.payload.response.NotificationContext.Action.VIEW;
import static space.nebula.nexus.payload.response.NotificationContext.ObjectType.COMMENT;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommentModerationEventListenerTest {
	@Test
	void approvedGuestReplyNotifiesRegisteredAudienceWithoutLookingUpGuest() {
		UserRepository userRepository = mock(UserRepository.class);
		INotificationService notificationService = mock(INotificationService.class);
		CommentModerationEventListener listener = new CommentModerationEventListener(userRepository,
				notificationService);
		User recipient = user(2L, "recipient");
		User postAuthor = user(3L, "post-author");
		when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
		when(userRepository.findById(3L)).thenReturn(Optional.of(postAuthor));
		var event = new CommentModeratedEvent(this, 48L, null, 2L, 3L, "Guest reader", "Example post",
				CommentStatus.APPROVED, "/posts/example#comment-48");

		listener.onCommentModerated(event);

		verify(userRepository).findById(2L);
		verify(userRepository).findById(3L);
		verifyNoMoreInteractions(userRepository);
		verify(notificationService).sendOnce(recipient, "New reply to your comment",
				"Guest reader replied to your comment.", NotificationType.COMMENT_REPLY, "/posts/example#comment-48",
				"COMMENT:48", new NotificationContext(COMMENT, 48L, null, VIEW));
		verify(notificationService).sendOnce(postAuthor, "New comment on your post",
				"Guest reader commented on \"Example post\".", NotificationType.POST_COMMENT,
				"/posts/example#comment-48", "COMMENT:48", new NotificationContext(COMMENT, 48L, null, VIEW));
		verifyNoMoreInteractions(notificationService);
	}

	@Test
	void rejectedGuestCommentDoesNotLookUpOrNotifyUsers() {
		UserRepository userRepository = mock(UserRepository.class);
		INotificationService notificationService = mock(INotificationService.class);
		CommentModerationEventListener listener = new CommentModerationEventListener(userRepository,
				notificationService);
		var event = new CommentModeratedEvent(this, 48L, null, 2L, 3L, "Guest reader", "Example post",
				CommentStatus.REJECTED, null);

		listener.onCommentModerated(event);

		verifyNoInteractions(userRepository, notificationService);
	}

	@Test
	void approvedReplyNotifiesAuthorReplyRecipientAndPostAuthor() {
		UserRepository userRepository = mock(UserRepository.class);
		INotificationService notificationService = mock(INotificationService.class);
		CommentModerationEventListener listener = new CommentModerationEventListener(userRepository,
				notificationService);
		User author = user(1L, "author");
		User recipient = user(2L, "recipient");
		User postAuthor = user(3L, "post-author");
		when(userRepository.findById(1L)).thenReturn(Optional.of(author));
		when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
		when(userRepository.findById(3L)).thenReturn(Optional.of(postAuthor));
		var event = new CommentModeratedEvent(this, 20L, 1L, 2L, 3L, "author", "Example post", CommentStatus.APPROVED,
				"/posts/example#comment-20");

		listener.onCommentModerated(event);

		verify(notificationService).sendOnce(author, "Comment approved",
				"Your comment is now visible to other readers.", NotificationType.COMMENT_APPROVED,
				"/posts/example#comment-20", event.getNotificationEventId(),
				new NotificationContext(COMMENT, 20L, event.getNotificationActorId(), VIEW));
		verify(notificationService).sendOnce(recipient, "New reply to your comment", "author replied to your comment.",
				NotificationType.COMMENT_REPLY, "/posts/example#comment-20", "COMMENT:20",
				new NotificationContext(COMMENT, 20L, 1L, VIEW));
		verify(notificationService).sendOnce(postAuthor, "New comment on your post",
				"author commented on \"Example post\".", NotificationType.POST_COMMENT, "/posts/example#comment-20",
				"COMMENT:20", new NotificationContext(COMMENT, 20L, 1L, VIEW));
	}

	@Test
	void approvedReplyOnlyNotifiesOnceWhenPostAuthorOwnsParentComment() {
		UserRepository userRepository = mock(UserRepository.class);
		INotificationService notificationService = mock(INotificationService.class);
		CommentModerationEventListener listener = new CommentModerationEventListener(userRepository,
				notificationService);
		User author = user(1L, "author");
		User recipient = user(2L, "post-author");
		when(userRepository.findById(1L)).thenReturn(Optional.of(author));
		when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
		var event = new CommentModeratedEvent(this, 20L, 1L, 2L, 2L, "author", "Example post", CommentStatus.APPROVED,
				"/posts/example#comment-20");

		listener.onCommentModerated(event);

		verify(notificationService).sendOnce(author, "Comment approved",
				"Your comment is now visible to other readers.", NotificationType.COMMENT_APPROVED,
				"/posts/example#comment-20", event.getNotificationEventId(),
				new NotificationContext(COMMENT, 20L, event.getNotificationActorId(), VIEW));
		verify(notificationService).sendOnce(recipient, "New reply to your comment", "author replied to your comment.",
				NotificationType.COMMENT_REPLY, "/posts/example#comment-20", "COMMENT:20",
				new NotificationContext(COMMENT, 20L, 1L, VIEW));
		verifyNoMoreInteractions(notificationService);
	}

	@Test
	void rejectedCommentNotifiesOnlyItsAuthor() {
		UserRepository userRepository = mock(UserRepository.class);
		INotificationService notificationService = mock(INotificationService.class);
		CommentModerationEventListener listener = new CommentModerationEventListener(userRepository,
				notificationService);
		User author = user(1L, "author");
		when(userRepository.findById(1L)).thenReturn(Optional.of(author));
		var event = new CommentModeratedEvent(this, 20L, 1L, 2L, 3L, "author", "Example post", CommentStatus.REJECTED,
				null);

		listener.onCommentModerated(event);

		verify(notificationService).sendOnce(author, "Comment not approved",
				"Your comment did not meet the publication requirements.", NotificationType.COMMENT_REJECTED, null,
				event.getNotificationEventId(),
				new NotificationContext(COMMENT, 20L, event.getNotificationActorId(), VIEW));
	}

	private User user(Long id, String username) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		return user;
	}

	@Test
	void approvedMomentCommentNotifiesMomentOwner() {
		UserRepository users = mock(UserRepository.class);
		INotificationService notifications = mock(INotificationService.class);
		User owner = user(3L, "owner");
		when(users.findById(3L)).thenReturn(Optional.of(owner));
		var event = new CommentModeratedEvent(this, 48L, null, null, 3L, "Guest", "Moment #7", CommentStatus.APPROVED,
				"/moments#comment-48", NotificationType.MOMENT_COMMENT);
		new CommentModerationEventListener(users, notifications).onCommentModerated(event);
		verify(notifications).sendOnce(owner, "New comment on your moment", "Guest commented on \"Moment #7\".",
				NotificationType.MOMENT_COMMENT, "/moments#comment-48", "COMMENT:48",
				new NotificationContext(COMMENT, 48L, null, VIEW));
	}

	@Test
	void approvedGuestbookMessageNotifiesAdministratorsWithoutDisclosingGuestContact() {
		UserRepository users = mock(UserRepository.class);
		INotificationService notifications = mock(INotificationService.class);
		var event = new CommentModeratedEvent(this, 48L, null, null, null, "Guest", null, CommentStatus.APPROVED,
				"/guestbook#comment-48", NotificationType.GUESTBOOK_COMMENT);
		new CommentModerationEventListener(users, notifications).onCommentModerated(event);
		verify(notifications).sendToAdministrators("New guestbook message", "A new guestbook message is now visible.",
				NotificationType.GUESTBOOK_COMMENT, "/guestbook#comment-48", "COMMENT:48", null,
				new NotificationContext(COMMENT, 48L, null, VIEW));
		verifyNoInteractions(users);
	}
}
