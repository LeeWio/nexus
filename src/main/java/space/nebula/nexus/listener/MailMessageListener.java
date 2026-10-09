package space.nebula.nexus.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.utils.MailUtil;
import space.nebula.nexus.service.NotificationDeliveryService;
import space.nebula.nexus.service.NewsletterDeliveryService;
import space.nebula.nexus.service.NotificationDeliveryService.DeliveryClaim;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;

/**
 * Asynchronous consumer for sending emails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailMessageListener {

	private final MailUtil mailUtil;
	private final NotificationDeliveryService notificationDeliveryService;
	private final NewsletterDeliveryService newsletterDeliveryService;
	@Value("${app.newsletter.base-url:http://localhost:3000}")
	private String applicationBaseUrl;

	@RabbitListener(queues = RabbitMQConfig.MAIL_QUEUE)
	public void processMailMessage(TemplateMailMessage message) {
		if (message.getNotificationDeliveryId() != null) {
			processNotification(message.getNotificationDeliveryId());
			return;
		}
		log.info("Received request to send async email to: {}, type: {}", message.getTo(), message.getType());
		try {
			switch (message.getType()) {
				case SIMPLE -> mailUtil.sendSimpleMail(message.getTo(), message.getSubject(), message.getContent());
				case HTML -> mailUtil.sendHtmlMail(message.getTo(), message.getSubject(), message.getContent());
				case TEMPLATE -> mailUtil.sendTemplateMail(message.getTo(), message.getSubject(),
						message.getTemplateName(), message.getVariables());
			}
			newsletterDeliveryService.markDelivered(message.getNewsletterDeliveryId());
			log.info("Async email successfully sent to: {}", message.getTo());
		} catch (Exception e) {
			newsletterDeliveryService.markFailed(message.getNewsletterDeliveryId(), e);
			log.error("Failed to send async email to: {}. Reason: {}", message.getTo(), e.getMessage(), e);
			// Rethrow exception to trigger RabbitMQ listener retry mechanism and eventual
			// DLQ routing
			throw new RuntimeException("Async email delivery failed", e);
		}
	}

	private void processNotification(Long deliveryId) {
		var claim = notificationDeliveryService.beginDelivery(deliveryId);
		if (claim.isEmpty())
			return;
		DeliveryClaim delivery = claim.get();
		try {
			String body = delivery.content();
			if (delivery.link() != null && !delivery.link().isBlank()) {
				URI base = URI.create(applicationBaseUrl.endsWith("/") ? applicationBaseUrl : applicationBaseUrl + "/");
				URI target = base.resolve(delivery.link());
				if (!java.util.Objects.equals(base.getHost(), target.getHost())
						|| !java.util.Objects.equals(base.getScheme(), target.getScheme())
						|| base.getPort() != target.getPort())
					throw new IllegalArgumentException("Notification link must belong to the application");
				body += "\n\nOpen in Odyssey: " + target;
			}
			mailUtil.sendSimpleMail(delivery.recipient(), "Odyssey - " + delivery.title(), body);
			notificationDeliveryService.markDelivered(delivery.id(), delivery.attempt());
		} catch (Exception error) {
			notificationDeliveryService.markFailed(delivery.id(), delivery.attempt(), error);
			log.warn("Notification delivery {} attempt {} failed: {}", delivery.id(), delivery.attempt(),
					error.getClass().getSimpleName());
			// The durable log owns retry timing for notifications; acknowledging avoids a
			// second retry budget.
		}
	}
}
