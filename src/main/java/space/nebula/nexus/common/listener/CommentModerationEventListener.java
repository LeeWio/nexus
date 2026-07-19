package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.INotificationService;

/**
 * Creates user notifications after comment moderation transactions commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentModerationEventListener
{
	private final UserRepository userRepository;
	private final INotificationService notificationService;

	/**
	 * Delivers moderation and reply notifications without coupling them to the moderation transaction.
	 *
	 * @param event committed comment moderation event
	 */
	@Async("asyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onCommentModerated(CommentModeratedEvent event)
	{
		userRepository.findById(event.getAuthorId()).ifPresent(author -> notifyAuthor(author, event));

		if (event.getStatus() == CommentStatus.APPROVED && event.getReplyRecipientId() != null
				&& !event.getReplyRecipientId().equals(event.getAuthorId()))
		{
			userRepository.findById(event.getReplyRecipientId()).ifPresent(recipient -> notificationService.send(recipient,
					"New reply to your comment", event.getAuthorUsername() + " replied to your comment.",
					"COMMENT_REPLY", event.getLink()));
		}
	}

	private void notifyAuthor(User author, CommentModeratedEvent event)
	{
		if (event.getStatus() == CommentStatus.APPROVED)
		{
			notificationService.send(author, "Comment approved", "Your comment is now visible to other readers.",
					"COMMENT_APPROVED", event.getLink());
		}
		else
		{
			notificationService.send(author, "Comment not approved",
					"Your comment did not meet the publication requirements.", "COMMENT_REJECTED", null);
		}
	}
}
