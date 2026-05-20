package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.common.validator.annotation.Password;

@Schema(description = "Request to change account password")
public record PasswordChangeRequest(
		@Schema(description = "Current account password", example = "OldPass123!") @NotBlank(message = "Old password is required") String oldPassword,

		@Schema(description = "New secure password", example = "NewSecurePass!2026") @NotBlank(message = "New password is required") @Size(min = 6, max = 32, message = "Password length must be between 6 and 32 characters") @Password String newPassword) {
}
