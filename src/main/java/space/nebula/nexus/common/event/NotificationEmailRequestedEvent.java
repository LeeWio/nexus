package space.nebula.nexus.common.event;

/**
 * Immutable request to deliver one notification email after the originating
 * business transaction commits successfully.
 */
public record NotificationEmailRequestedEvent(Long deliveryId, String recipientEmail, String title, String content,
		String link, int generation) {
	public NotificationEmailRequestedEvent(Long deliveryId, String recipientEmail, String title, String content,
			String link) {
		this(deliveryId, recipientEmail, title, content, link, 0);
	}
}
