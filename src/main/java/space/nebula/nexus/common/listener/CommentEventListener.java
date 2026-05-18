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

import java.util.HashMap;
import java.util.Map;

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
        
        // Target email is post author, or admin if guestbook/missing
        String targetEmail = null;
        String authorName = "Admin";
        String postTitle = "Guestbook";

        if (comment.getPost() != null) {
            targetEmail = comment.getPost().getAuthor().getEmail();
            authorName = comment.getPost().getAuthor().getNickname() != null 
                    ? comment.getPost().getAuthor().getNickname() 
                    : comment.getPost().getAuthor().getUsername();
            postTitle = comment.getPost().getTitle();
        }

        if (targetEmail == null || targetEmail.isBlank()) {
            targetEmail = "admin@nexus.com"; 
        }

        if (comment.getStatus() == CommentStatus.REJECTED) {
            sendViolationAlert(comment, targetEmail, postTitle);
        } else {
            sendNewCommentNotification(comment, targetEmail, authorName, postTitle);
        }
    }

    private void sendNewCommentNotification(Comment comment, String email, String authorName, String postTitle) {
        log.info("Sending HTML comment notification email to: {}", email);
        String subject = "New Comment on: " + postTitle;
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("authorName", authorName);
        variables.put("commenterName", comment.getUser().getNickname() != null ? comment.getUser().getNickname() : comment.getUser().getUsername());
        variables.put("postTitle", postTitle);
        variables.put("commentContent", comment.getContent());

        mailUtil.sendTemplateMail(email, subject, "new-comment", variables);
    }

    private void sendViolationAlert(Comment comment, String email, String postTitle) {
        log.info("Sending HTML violation alert email to admin: {}", email);
        String subject = "[ALERT] Content Violation Blocked on: " + postTitle;

        Map<String, Object> variables = new HashMap<>();
        variables.put("commenterName", comment.getUser().getUsername());
        variables.put("postTitle", postTitle);
        variables.put("commentContent", comment.getContent());
        variables.put("ipAddress", comment.getIpAddress());
        variables.put("userAgent", comment.getUserAgent());

        mailUtil.sendTemplateMail(email, subject, "violation-alert", variables);
    }
}
