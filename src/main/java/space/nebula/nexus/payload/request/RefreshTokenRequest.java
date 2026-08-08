package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to exchange a refresh token for a new authentication token pair")
public record RefreshTokenRequest(
		@Schema(description = "JWT refresh token", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "Refresh token is required") String refreshToken) {
}
