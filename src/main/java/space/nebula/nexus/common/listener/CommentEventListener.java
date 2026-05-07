package space.nebula.nexus.common.listener;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.utils.MailUtil;

/**
 * Listener for comment-related events.
 */
@Slf4j
@Component
public class CommentEventListener {

    @Resource
    private MailUtil mailUtil;

    /**
     * Handle comment submission. Dispatched asynchronously via 'asyncExecutor'.
     */
    @Async("asyncExecutor")
    @EventListener
    public void onCommentSubmitted(CommentSubmittedEvent event) {
        Comment comment = event.getComment();
        String authorEmail = comment.getPost().getAuthor().getEmail();

        if (authorEmail == null || authorEmail.isBlank()) {
            log.warn("Cannot send notification: Author of post [{}] has no email address.", 
                    comment.getPost().getTitle());
            return;
        }

        if (comment.getStatus() == CommentStatus.REJECTED) {
            sendViolationAlert(comment, authorEmail);
        } else {
            sendNewCommentNotification(comment, authorEmail);
        }
    }

    private void sendNewCommentNotification(Comment comment, String authorEmail) {
        log.info("Sending comment notification email to: {}", authorEmail);
        String subject = "New Comment on your post: " + comment.getPost().getTitle();
        String content = String.format(
                "Hello %s,\n\nUser '%s' has left a new comment on your post '%s'.\n\nContent:\n%s\n\n" +
                "Please log in to the admin panel to moderate this comment.",
                comment.getPost().getAuthor().getNickname(),
                comment.getUser().getUsername(),
                comment.getPost().getTitle(),
                comment.getContent()
        );
        mailUtil.sendSimpleMail(authorEmail, subject, content);
    }

    private void sendViolationAlert(Comment comment, String adminEmail) {
        log.info("Sending violation alert email to admin: {}", adminEmail);
        String subject = "[ALERT] Content Violation Blocked: " + comment.getPost().getTitle();
        String content = String.format(
                "System Alert,\n\nA comment by user '%s' was automatically REJECTED on post '%s' due to sensitive content.\n\n" +
                "Blocked Content (Masked):\n%s\n\n" +
                "User IP: %s\n" +
                "User Agent: %s\n\n" +
                "No action is required as the comment is hidden from the public.",
                comment.getUser().getUsername(),
                comment.getPost().getTitle(),
                comment.getContent(),
                comment.getIpAddress(),
                comment.getUserAgent()
        );
        mailUtil.sendSimpleMail(adminEmail, subject, content);
    }
}
