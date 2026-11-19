package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import space.nebula.nexus.enums.WebhookEvent;

import java.util.List;

public record WebhookUpdateRequest(@NotBlank String name, @NotBlank String url, String secret,
		@NotEmpty List<WebhookEvent> events, @NotNull Boolean isActive) {
}
