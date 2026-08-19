package space.nebula.nexus.common.listener;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.INotificationService;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class CommentModerationEventListenerTest {
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

		verify(notificationService).send(author, "Comment approved", "Your comment is now visible to other readers.",
				"COMMENT_APPROVED", "/posts/example#comment-20");
		verify(notificationService).send(recipient, "New reply to your comment", "author replied to your comment.",
				"COMMENT_REPLY", "/posts/example#comment-20");
		verify(notificationService).send(postAuthor, "New comment on your post",
				"author commented on \"Example post\".", "POST_COMMENT", "/posts/example#comment-20");
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

		verify(notificationService).send(author, "Comment approved", "Your comment is now visible to other readers.",
				"COMMENT_APPROVED", "/posts/example#comment-20");
		verify(notificationService).send(recipient, "New reply to your comment", "author replied to your comment.",
				"COMMENT_REPLY", "/posts/example#comment-20");
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

		verify(notificationService).send(author, "Comment not approved",
				"Your comment did not meet the publication requirements.", "COMMENT_REJECTED", null);
	}

	private User user(Long id, String username) {
		User user = new User();
		user.setId(id);
		user.setUsername(username);
		return user;
	}
}
