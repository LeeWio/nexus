package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.TemplateMailMessage;

import cn.hutool.core.lang.Dict;

import java.util.Map;

/**
 * Listener for comment-related events. Dispatches email notifications via RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventListener {

	private final RabbitTemplate rabbitTemplate;

	/**
	 * Handle comment submission. Dispatched asynchronously via 'asyncExecutor'.
	 */
	@Async("asyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
		String subject = "New Comment on: " + postTitle;

		Map<String, Object> variables = Dict.create()
				.set("authorName", authorName)
				.set("commenterName",
						comment.getUser().getNickname() != null
								? comment.getUser().getNickname()
								: comment.getUser().getUsername())
				.set("postTitle", postTitle)
				.set("commentContent", comment.getContent());

		TemplateMailMessage message = TemplateMailMessage.builder()
				.to(email)
				.subject(subject)
				.templateName("new-comment")
				.variables(variables)
				.type(TemplateMailMessage.MailType.TEMPLATE)
				.build();

		rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, message);
		log.info("Dispatched async comment notification task for: {}", email);
	}

	private void sendViolationAlert(Comment comment, String email, String postTitle) {
		String subject = "[ALERT] Content Violation Blocked on: " + postTitle;

		Map<String, Object> variables = Dict.create()
				.set("commenterName", comment.getUser().getUsername())
				.set("postTitle", postTitle)
				.set("commentContent", comment.getContent())
				.set("ipAddress", comment.getIpAddress())
				.set("userAgent", comment.getUserAgent());

		TemplateMailMessage message = TemplateMailMessage.builder()
				.to(email)
				.subject(subject)
				.templateName("violation-alert")
				.variables(variables)
				.type(TemplateMailMessage.MailType.TEMPLATE)
				.build();

		rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, message);
		log.info("Dispatched async violation alert task for: {}", email);
	}
}
