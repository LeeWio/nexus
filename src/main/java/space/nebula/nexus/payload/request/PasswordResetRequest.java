package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request a password-reset verification code. The response is identical whether or not the email belongs to an account.")
public record PasswordResetRequest(
		@Schema(description = "Email address associated with the account", example = "user@example.com") @NotBlank(message = "Email is required") @Email(message = "Email address is invalid") @Size(max = 100) String email) {
}
