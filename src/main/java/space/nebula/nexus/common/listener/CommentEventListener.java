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
		// Target email is post author, or admin if guestbook/missing
		String targetEmail = event.getPostAuthorEmail();
		String authorName = event.getPostAuthorDisplayName() != null ? event.getPostAuthorDisplayName() : "Admin";
		String postTitle = event.getPostTitle();

		if (targetEmail == null || targetEmail.isBlank()) {
			targetEmail = "admin@nexus.com";
		}

		if (event.getStatus() == CommentStatus.SPAM) {
			sendViolationAlert(event, targetEmail, postTitle);
		} else {
			sendNewCommentNotification(event, targetEmail, authorName, postTitle);
		}
	}

	private void sendNewCommentNotification(CommentSubmittedEvent event, String email, String authorName, String postTitle) {
		String subject = "New Comment on: " + postTitle;

		Map<String, Object> variables = Dict.create()
				.set("authorName", authorName)
				.set("commenterName", event.getAuthorDisplayName())
				.set("postTitle", postTitle)
				.set("commentContent", event.getContent());

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

	private void sendViolationAlert(CommentSubmittedEvent event, String email, String postTitle) {
		String subject = "[ALERT] Content Violation Blocked on: " + postTitle;

		Map<String, Object> variables = Dict.create()
				.set("commenterName", event.getAuthorUsername())
				.set("postTitle", postTitle)
				.set("commentContent", event.getContent())
				.set("ipAddress", event.getIpAddress())
				.set("userAgent", event.getUserAgent());

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
