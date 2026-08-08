package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.common.validator.annotation.Password;

/**
 * Register request payload using Java 21 Record.
 */
@Schema(description = "User registration request information")
public record RegisterRequest(
		@Schema(description = "Desired username", example = "john_doe", minLength = 3, maxLength = 20) @NotBlank(message = "Username is required") @Size(min = 3, max = 20) String username,

		@Schema(description = "User email address", example = "john@example.com") @NotBlank(message = "Email is required") @Email(message = "Email address is invalid") @Size(max = 50) String email,

		@Schema(description = "User password. Must contain upper-case and lower-case letters, a digit, and a special character.", example = "SecurePass123!") @NotBlank(message = "Password is required") @Password String password) {
}
