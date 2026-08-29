package space.nebula.nexus.common.event;

/**
 * Immutable request to deliver one notification email after the originating
 * business transaction commits successfully.
 */
public record NotificationEmailRequestedEvent(Long deliveryId, String recipientEmail, String title, String content, String link) {
}
