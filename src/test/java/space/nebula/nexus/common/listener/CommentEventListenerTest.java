package space.nebula.nexus.common.listener;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.NotificationType;
import space.nebula.nexus.payload.response.NotificationContext;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.INotificationService;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static space.nebula.nexus.payload.response.NotificationContext.Action.REVIEW;
import static space.nebula.nexus.payload.response.NotificationContext.Action.VIEW;
import static space.nebula.nexus.payload.response.NotificationContext.ObjectType.COMMENT;

class CommentEventListenerTest {
	@Test
	void pendingGuestCommentNotifiesModeratorsThroughPreferenceAwareService() {
		CommentRepository comments = mock(CommentRepository.class);
		INotificationService notifications = mock(INotificationService.class);
		CommentModerationEventListener moderation = mock(CommentModerationEventListener.class);
		Comment comment = new Comment();
		comment.setId(100L);
		when(comments.findById(100L)).thenReturn(Optional.of(comment));
		var event = event(CommentStatus.PENDING);
		new CommentEventListener(comments, notifications, moderation).onCommentSubmitted(event);
		verify(notifications).sendToAdministrators("Comment awaiting review",
				"A comment on \"Guestbook\" needs moderation.", NotificationType.COMMENT_PENDING_REVIEW,
				"/comments?id=100", event.getNotificationEventId(), null,
				new NotificationContext(COMMENT, 100L, null, REVIEW));
		verifyNoInteractions(moderation);
	}

	@Test
	void flaggedCommentDoesNotIncludeNetworkOrPrivateCommentDataInAlert() {
		CommentRepository comments = mock(CommentRepository.class);
		INotificationService notifications = mock(INotificationService.class);
		Comment comment = new Comment();
		comment.setId(100L);
		when(comments.findById(100L)).thenReturn(Optional.of(comment));
		var event = event(CommentStatus.SPAM);
		new CommentEventListener(comments, notifications, mock(CommentModerationEventListener.class))
				.onCommentSubmitted(event);
		verify(notifications).sendToAdministrators("Comment flagged for review",
				"A comment on \"Guestbook\" needs moderation.", NotificationType.COMMENT_FLAGGED, "/comments?id=100",
				event.getNotificationEventId(), null, new NotificationContext(COMMENT, 100L, null, REVIEW));
	}

	@Test
	void immediatelyApprovedReplyNotifiesParentAuthorWithoutApprovalReceipt() {
		CommentRepository comments = mock(CommentRepository.class);
		UserRepository users = mock(UserRepository.class);
		INotificationService notifications = mock(INotificationService.class);
		User admin = new User();
		admin.setId(1L);
		User reader = new User();
		reader.setId(2L);
		Comment parent = new Comment();
		parent.setUser(reader);
		Comment comment = new Comment();
		comment.setId(100L);
		comment.setUser(admin);
		comment.setParent(parent);
		when(comments.findById(100L)).thenReturn(Optional.of(comment));
		when(users.findById(2L)).thenReturn(Optional.of(reader));
		new CommentEventListener(comments, notifications, new CommentModerationEventListener(users, notifications))
				.onCommentSubmitted(event(CommentStatus.APPROVED));
		verify(notifications).sendOnce(reader, "New reply to your comment", "Admin replied to your comment.",
				NotificationType.COMMENT_REPLY, "/guestbook#comment-100", "COMMENT:100",
				new NotificationContext(COMMENT, 100L, 1L, VIEW));
		verifyNoMoreInteractions(notifications);
	}

	private CommentSubmittedEvent event(CommentStatus status) {
		return new CommentSubmittedEvent(this, 100L, "admin", "Admin", "Private content", status, "Guestbook", null,
				null, "127.0.0.1", "JUnit");
	}
}
