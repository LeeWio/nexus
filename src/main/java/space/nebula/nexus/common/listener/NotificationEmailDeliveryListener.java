package space.nebula.nexus.common.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import space.nebula.nexus.common.event.NotificationEmailRequestedEvent;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.service.NotificationDeliveryService;

/**
 * Hands committed notification emails to the existing retryable mail queue.
 */
@Component
@RequiredArgsConstructor
public class NotificationEmailDeliveryListener {

	private final RabbitTemplate rabbitTemplate;
	private final NotificationDeliveryService notificationDeliveryService;

	@Async("asyncExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onNotificationEmailRequested(NotificationEmailRequestedEvent event) {
		String body = event.content()
				+ (event.link() == null || event.link().isBlank() ? "" : "\n\nOpen in Odyssey: " + event.link());
		TemplateMailMessage message = TemplateMailMessage.builder().to(event.recipientEmail())
				.subject("Odyssey — " + event.title()).content(body).type(TemplateMailMessage.MailType.SIMPLE)
				.notificationDeliveryId(event.deliveryId()).build();
		try {
			rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, message);
		} catch (RuntimeException error) {
			notificationDeliveryService.markFailed(event.deliveryId(), error);
		}
	}
}
