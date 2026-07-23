package space.nebula.nexus.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import space.nebula.nexus.enums.CommentStatus;

/**
 * Event published when a new comment is submitted.
 */
@Getter
public class CommentSubmittedEvent extends ApplicationEvent {

	private final Long commentId;
	private final String authorUsername;
	private final String authorDisplayName;
	private final String content;
	private final CommentStatus status;
	private final String postTitle;
	private final String postAuthorEmail;
	private final String postAuthorDisplayName;
	private final String ipAddress;
	private final String userAgent;

	public CommentSubmittedEvent(Object source, Long commentId, String authorUsername, String authorDisplayName,
			String content, CommentStatus status, String postTitle, String postAuthorEmail,
			String postAuthorDisplayName, String ipAddress, String userAgent) {
		super(source);
		this.commentId = commentId;
		this.authorUsername = authorUsername;
		this.authorDisplayName = authorDisplayName;
		this.content = content;
		this.status = status;
		this.postTitle = postTitle;
		this.postAuthorEmail = postAuthorEmail;
		this.postAuthorDisplayName = postAuthorDisplayName;
		this.ipAddress = ipAddress;
		this.userAgent = userAgent;
	}
}
