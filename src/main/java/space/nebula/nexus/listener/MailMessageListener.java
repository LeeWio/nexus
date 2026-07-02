package space.nebula.nexus.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.utils.MailUtil;

/**
 * Asynchronous consumer for sending emails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailMessageListener
{

	private final MailUtil mailUtil;

	@RabbitListener(queues = RabbitMQConfig.MAIL_QUEUE)
	public void processMailMessage(TemplateMailMessage message)
	{
		log.info("Received request to send async email to: {}, type: {}", message.getTo(), message.getType());
		try
		{
			switch (message.getType()) {
			case SIMPLE -> mailUtil.sendSimpleMail(message.getTo(), message.getSubject(), message.getContent());
			case HTML -> mailUtil.sendHtmlMail(message.getTo(), message.getSubject(), message.getContent());
			case TEMPLATE -> mailUtil.sendTemplateMail(message.getTo(), message.getSubject(), message.getTemplateName(),
					message.getVariables());
			}
			log.info("Async email successfully sent to: {}", message.getTo());
		}
		catch (Exception e)
		{
			log.error("Failed to send async email to: {}. Reason: {}", message.getTo(), e.getMessage(), e);
			// Rethrow exception to trigger RabbitMQ listener retry mechanism and eventual DLQ routing
			throw new RuntimeException("Async email delivery failed", e);
		}
	}
}
