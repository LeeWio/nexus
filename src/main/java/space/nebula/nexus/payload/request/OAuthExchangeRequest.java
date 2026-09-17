package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Exchange a one-time OAuth login code for access and refresh tokens")
public record OAuthExchangeRequest(
		@Schema(description = "Opaque one-time OAuth login code", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "OAuth login code is required") String code) {
}
