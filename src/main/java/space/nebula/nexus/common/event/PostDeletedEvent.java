package space.nebula.nexus.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when a post is deleted.
 */
@Getter
public class PostDeletedEvent extends ApplicationEvent {
    
    private final Long postId;
    private final String slug;

    public PostDeletedEvent(Object source, Long postId, String slug) {
        super(source);
        this.postId = postId;
        this.slug = slug;
    }
}
