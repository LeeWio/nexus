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
	private final boolean isNew;

	public PostChangedEvent(Object source, Post post, boolean isNew) {
		super(source);
		this.post = post;
		this.isNew = isNew;
	}
}
