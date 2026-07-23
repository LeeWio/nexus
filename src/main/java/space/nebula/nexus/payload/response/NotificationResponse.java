package space.nebula.nexus.payload.response;

import java.time.LocalDateTime;

/**
 * Safe API representation of a user notification without recipient account
 * data.
 */
public record NotificationResponse(Long id, String title, String content, String type, Boolean read,
		LocalDateTime readAt, String link, LocalDateTime createdAt) {
}
