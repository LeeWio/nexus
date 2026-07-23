package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import space.nebula.nexus.enums.WebhookEvent;

import java.util.List;

public record WebhookRequest(@NotBlank(message = "Name cannot be blank") String name,
		@NotBlank(message = "URL cannot be blank") String url,
		@NotBlank(message = "Secret cannot be blank") String secret,
		@NotEmpty(message = "At least one event must be selected") List<WebhookEvent> events,
		@NotNull(message = "Active status must be provided") Boolean isActive) {
}
