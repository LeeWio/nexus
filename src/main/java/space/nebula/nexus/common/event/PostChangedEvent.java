package space.nebula.nexus.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import space.nebula.nexus.entity.Post;

/**
 * Event published when a post is created or updated.
 */
@Getter
public class PostChangedEvent extends ApplicationEvent {

	private final Post post;
	private final PostChangeType changeType;
	private final String previousSlug;

	public PostChangedEvent(Object source, Post post, PostChangeType changeType) {
		this(source, post, changeType, post.getSlug());
	}

	public PostChangedEvent(Object source, Post post, PostChangeType changeType, String previousSlug) {
		super(source);
		this.post = post;
		this.changeType = changeType;
		this.previousSlug = previousSlug;
	}
}
