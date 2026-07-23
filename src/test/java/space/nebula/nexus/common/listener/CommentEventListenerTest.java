package space.nebula.nexus.common.listener;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.enums.CommentStatus;
import space.nebula.nexus.payload.request.TemplateMailMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CommentEventListenerTest {
	@Test
	void submittedCommentUsesScalarEventPayloadForNotification() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		CommentEventListener listener = new CommentEventListener(rabbitTemplate);
		var event = new CommentSubmittedEvent(this, 100L, "reader", "Reader", "Hello", CommentStatus.PENDING,
				"Post Title", "author@example.com", "Author", "127.0.0.1", "JUnit");

		listener.onCommentSubmitted(event);

		ArgumentCaptor<TemplateMailMessage> messageCaptor = ArgumentCaptor.forClass(TemplateMailMessage.class);
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.MAIL_EXCHANGE), eq(RabbitMQConfig.MAIL_ROUTING_KEY),
				messageCaptor.capture());
		TemplateMailMessage message = messageCaptor.getValue();
		assertEquals("author@example.com", message.getTo());
		assertEquals("new-comment", message.getTemplateName());
		assertEquals("Reader", message.getVariables().get("commenterName"));
		assertEquals("Hello", message.getVariables().get("commentContent"));
	}

	@Test
	void spamCommentDispatchesViolationAlertToFallbackAdmin() {
		RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
		CommentEventListener listener = new CommentEventListener(rabbitTemplate);
		var event = new CommentSubmittedEvent(this, 100L, "spammer", "Spammer", "***", CommentStatus.SPAM, "Guestbook",
				null, null, "127.0.0.1", "JUnit");

		listener.onCommentSubmitted(event);

		ArgumentCaptor<TemplateMailMessage> messageCaptor = ArgumentCaptor.forClass(TemplateMailMessage.class);
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.MAIL_EXCHANGE), eq(RabbitMQConfig.MAIL_ROUTING_KEY),
				messageCaptor.capture());
		TemplateMailMessage message = messageCaptor.getValue();
		assertEquals("admin@nexus.com", message.getTo());
		assertEquals("violation-alert", message.getTemplateName());
		assertEquals("spammer", message.getVariables().get("commenterName"));
		assertEquals("127.0.0.1", message.getVariables().get("ipAddress"));
	}
}
