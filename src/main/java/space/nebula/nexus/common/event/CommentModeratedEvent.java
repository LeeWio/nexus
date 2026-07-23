package space.nebula.nexus.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import space.nebula.nexus.enums.CommentStatus;

/**
 * Event published after a comment moderation decision is persisted.
 */
@Getter
public class CommentModeratedEvent extends ApplicationEvent {
	private final Long commentId;
	private final Long authorId;
	private final Long replyRecipientId;
	private final String authorUsername;
	private final CommentStatus status;
	private final String link;

	/**
	 * Creates an immutable moderation event containing only notification data.
	 *
	 * @param source
	 *            event publisher
	 * @param commentId
	 *            moderated comment identifier
	 * @param authorId
	 *            comment author identifier
	 * @param replyRecipientId
	 *            parent comment author identifier, or {@code null}
	 * @param authorUsername
	 *            comment author's display identifier
	 * @param status
	 *            moderation outcome
	 * @param link
	 *            frontend link to the approved comment, or {@code null}
	 */
	public CommentModeratedEvent(Object source, Long commentId, Long authorId, Long replyRecipientId,
			String authorUsername, CommentStatus status, String link) {
		super(source);
		this.commentId = commentId;
		this.authorId = authorId;
		this.replyRecipientId = replyRecipientId;
		this.authorUsername = authorUsername;
		this.status = status;
		this.link = link;
	}
}
