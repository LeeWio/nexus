package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.NotificationType;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.INotificationService;
import space.nebula.nexus.payload.response.NotificationContext;
import static space.nebula.nexus.payload.response.NotificationContext.ObjectType.COMMENT;
import static space.nebula.nexus.payload.response.NotificationContext.Action.VIEW;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Persists user notifications in the comment moderation transaction.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentModerationEventListener {
	private final UserRepository userRepository;
	private final INotificationService notificationService;

	/**
	 * Records moderation results and audience notifications before commit. Email
	 * delivery remains asynchronous after commit.
	 *
	 * @param event
	 *            comment moderation event
	 */
	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onCommentModerated(CommentModeratedEvent event) {
		if (event.getAuthorId() != null) {
			userRepository.findById(event.getAuthorId()).ifPresent(author -> notifyAuthor(author, event));
		}

		if (event.getStatus() == CommentStatus.APPROVED) {
			notifyApprovedCommentAudience(event);
		}
	}

	void notifyApprovedCommentAudience(CommentModeratedEvent event) {
		Set<Long> notifiedRecipients = new LinkedHashSet<>();
		if (isAnotherUser(event.getReplyRecipientId(), event.getAuthorId())
				&& notifiedRecipients.add(event.getReplyRecipientId())) {
			userRepository.findById(event.getReplyRecipientId())
					.ifPresent(recipient -> notificationService.sendOnce(recipient, "New reply to your comment",
							event.getAuthorUsername() + " replied to your comment.", NotificationType.COMMENT_REPLY,
							event.getLink(), "COMMENT:" + event.getCommentId(),
							new NotificationContext(COMMENT, event.getCommentId(), event.getAuthorId(), VIEW)));
		}
		if (isAnotherUser(event.getPostAuthorId(), event.getAuthorId())
				&& notifiedRecipients.add(event.getPostAuthorId())) {
			userRepository.findById(event.getPostAuthorId())
					.ifPresent(recipient -> notificationService.sendOnce(recipient,
							event.getAudienceType() == NotificationType.MOMENT_COMMENT
									? "New comment on your moment"
									: "New comment on your post",
							event.getAuthorUsername() + " commented on \"" + event.getPostTitle() + "\".",
							event.getAudienceType(), event.getLink(), "COMMENT:" + event.getCommentId(),
							new NotificationContext(COMMENT, event.getCommentId(), event.getAuthorId(), VIEW)));
		}
		if (event.getAudienceType() == NotificationType.GUESTBOOK_COMMENT && event.getReplyRecipientId() == null) {
			notificationService.sendToAdministrators("New guestbook message", "A new guestbook message is now visible.",
					NotificationType.GUESTBOOK_COMMENT, event.getLink(), "COMMENT:" + event.getCommentId(),
					event.getAuthorId(),
					new NotificationContext(COMMENT, event.getCommentId(), event.getAuthorId(), VIEW));
		}
	}

	private boolean isAnotherUser(Long recipientId, Long authorId) {
		return recipientId != null && !recipientId.equals(authorId);
	}

	private void notifyAuthor(User author, CommentModeratedEvent event) {
		if (event.getStatus() == CommentStatus.APPROVED) {
			notificationService.sendOnce(author, "Comment approved", "Your comment is now visible to other readers.",
					NotificationType.COMMENT_APPROVED, event.getLink(), event.getNotificationEventId(),
					new NotificationContext(COMMENT, event.getCommentId(), event.getNotificationActorId(), VIEW));
		} else {
			notificationService.sendOnce(author, "Comment not approved",
					"Your comment did not meet the publication requirements.", NotificationType.COMMENT_REJECTED, null,
					event.getNotificationEventId(),
					new NotificationContext(COMMENT, event.getCommentId(), event.getNotificationActorId(), VIEW));
		}
	}
}
