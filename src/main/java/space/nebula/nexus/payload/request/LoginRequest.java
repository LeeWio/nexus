package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload using Java 21 Record.
 */
@Schema(description = "User login request information")
public record LoginRequest(
		@Schema(description = "Username or email address", example = "admin") @NotBlank(message = "Username is required") String username,

		@Schema(description = "User password", example = "Password123!") @NotBlank(message = "Password is required") String password) {
}
