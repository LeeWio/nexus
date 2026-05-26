package space.nebula.nexus.payload.response;

import lombok.Builder;
import space.nebula.nexus.enums.WebhookEvent;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record WebhookResponse(
		Long id,
		String name,
		String url,
		List<WebhookEvent> events,
		Boolean isActive,
		LocalDateTime createdAt
) {}
