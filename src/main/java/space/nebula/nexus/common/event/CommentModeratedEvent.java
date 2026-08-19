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
	private final Long postAuthorId;
	private final String authorUsername;
	private final String postTitle;
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
	 * @param postAuthorId
	 *            article author identifier, or {@code null} for guestbook comments
	 * @param authorUsername
	 *            comment author's display identifier
	 * @param postTitle
	 *            article title, or {@code null} for guestbook comments
	 * @param status
	 *            moderation outcome
	 * @param link
	 *            frontend link to the approved comment, or {@code null}
	 */
	public CommentModeratedEvent(Object source, Long commentId, Long authorId, Long replyRecipientId, Long postAuthorId,
			String authorUsername, String postTitle, CommentStatus status, String link) {
		super(source);
		this.commentId = commentId;
		this.authorId = authorId;
		this.replyRecipientId = replyRecipientId;
		this.postAuthorId = postAuthorId;
		this.authorUsername = authorUsername;
		this.postTitle = postTitle;
		this.status = status;
		this.link = link;
	}
}
