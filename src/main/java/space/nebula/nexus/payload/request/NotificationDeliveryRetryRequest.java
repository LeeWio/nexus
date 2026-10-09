package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

public record NotificationDeliveryRetryRequest(
		@Schema(description = "Client-generated UUID. Reuse the same UUID when retrying an uncertain HTTP response.") @NotNull UUID requestId,
		@NotBlank @Size(max = 500) String reason) {
}
