package space.nebula.nexus.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * Payload for sending emails via RabbitMQ. Supports simple, HTML, and
 * template-based emails.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMailMessage implements Serializable {
	private String to;
	private String subject;
	private String content; // For simple or HTML emails
	private String templateName; // For template-based emails
	private Map<String, Object> variables; // For template-based emails
	private MailType type;

	public enum MailType {
		SIMPLE, HTML, TEMPLATE
	}
}
