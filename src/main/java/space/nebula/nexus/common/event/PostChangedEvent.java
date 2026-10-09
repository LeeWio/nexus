package space.nebula.nexus.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostStatus;

/**
 * Event published when a post is created or updated.
 */
@Getter
public class PostChangedEvent extends ApplicationEvent {
	private final String notificationEventId = java.util.UUID.randomUUID().toString();
	private final Long notificationActorId = space.nebula.nexus.security.util.SecurityUtil.getCurrentUser() == null
			? null
			: space.nebula.nexus.security.util.SecurityUtil.getCurrentUser().getId();

	private final Post post;
	private final PostChangeType changeType;
	private final String previousSlug;
	private final PostStatus previousStatus;

	public PostChangedEvent(Object source, Post post, PostChangeType changeType) {
		this(source, post, changeType, post.getSlug(), post.getStatus());
	}

	public PostChangedEvent(Object source, Post post, PostChangeType changeType, String previousSlug) {
		this(source, post, changeType, previousSlug, post.getStatus());
	}

	public PostChangedEvent(Object source, Post post, PostChangeType changeType, String previousSlug,
			PostStatus previousStatus) {
		super(source);
		this.post = post;
		this.changeType = changeType;
		this.previousSlug = previousSlug;
		this.previousStatus = previousStatus;
	}
}
