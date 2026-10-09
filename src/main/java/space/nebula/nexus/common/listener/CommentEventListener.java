package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.CommentModeratedEvent;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.enums.NotificationType;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.service.INotificationService;
import space.nebula.nexus.payload.response.NotificationContext;
import static space.nebula.nexus.payload.response.NotificationContext.ObjectType.COMMENT;
import static space.nebula.nexus.payload.response.NotificationContext.Action.REVIEW;

/**
 * Persists submission alerts with the comment, including immediately approved
 * replies.
 */
@Component
@RequiredArgsConstructor
public class CommentEventListener {
	private final CommentRepository commentRepository;
	private final INotificationService notificationService;
	private final CommentModerationEventListener moderationListener;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void onCommentSubmitted(CommentSubmittedEvent event) {
		commentRepository.findById(event.getCommentId()).ifPresent(comment -> {
			Long authorId = comment.getUser() == null ? null : comment.getUser().getId();
			if (event.getStatus() == CommentStatus.APPROVED) {
				var parent = comment.getParent();
				Long replyId = parent == null || parent.getUser() == null ? null : parent.getUser().getId();
				var post = comment.getPost();
				Long postAuthorId = post == null || post.getAuthor() == null ? null : post.getAuthor().getId();
				if (comment.getMoment() != null && comment.getMoment().getUser() != null)
					postAuthorId = comment.getMoment().getUser().getId();
				NotificationType audienceType = comment.getMoment() != null
						? NotificationType.MOMENT_COMMENT
						: post == null ? NotificationType.GUESTBOOK_COMMENT : NotificationType.POST_COMMENT;
				String base = comment.getMoment() != null
						? "/moments"
						: post == null ? "/guestbook" : "/posts/" + post.getSlug();
				moderationListener.notifyApprovedCommentAudience(new CommentModeratedEvent(this, comment.getId(),
						authorId, replyId, postAuthorId, event.getAuthorDisplayName(), event.getPostTitle(),
						CommentStatus.APPROVED, base + "#comment-" + comment.getId(), audienceType));
			} else if (event.getStatus() == CommentStatus.PENDING || event.getStatus() == CommentStatus.SPAM) {
				boolean flagged = event.getStatus() == CommentStatus.SPAM;
				notificationService.sendToAdministrators(
						flagged ? "Comment flagged for review" : "Comment awaiting review",
						"A comment on \"" + event.getPostTitle() + "\" needs moderation.",
						flagged ? NotificationType.COMMENT_FLAGGED : NotificationType.COMMENT_PENDING_REVIEW,
						"/comments?id=" + comment.getId(), event.getNotificationEventId(), authorId,
						new NotificationContext(COMMENT, comment.getId(), authorId, REVIEW));
			}
		});
	}
}
