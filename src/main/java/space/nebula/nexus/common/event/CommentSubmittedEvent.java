package space.nebula.nexus.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import space.nebula.nexus.entity.Comment;

/**
 * Event published when a new comment is submitted.
 */
@Getter
public class CommentSubmittedEvent extends ApplicationEvent {

    private final Comment comment;

    public CommentSubmittedEvent(Object source, Comment comment) {
        super(source);
        this.comment = comment;
    }
}
