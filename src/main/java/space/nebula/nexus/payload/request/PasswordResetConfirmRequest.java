package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.common.validator.annotation.Password;

@Schema(description = "Confirm a password reset with the one-time code sent to the account email")
public record PasswordResetConfirmRequest(
		@Schema(description = "Email address used for the reset request", example = "user@example.com") @NotBlank(message = "Email is required") @Email(message = "Email address is invalid") @Size(max = 100) String email,

		@Schema(description = "Six-digit password-reset verification code", example = "123456", pattern = "\\d{6}") @NotBlank(message = "Verification code is required") @Pattern(regexp = "\\d{6}", message = "Verification code must contain 6 digits") String code,

		@Schema(description = "New password. Must contain upper-case and lower-case letters, a digit, and a special character.", example = "NewP@ssw0rd123!") @NotBlank(message = "New password is required") @Password String newPassword) {
}
