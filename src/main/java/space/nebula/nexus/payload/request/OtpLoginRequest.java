package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to login using email and One-Time Password")
public record OtpLoginRequest(
		@Schema(description = "User's email address", example = "user@example.com") @NotBlank(message = "Email is required") @Email(message = "Email address is invalid") String email,

		@Schema(description = "The 6-digit OTP code received via email", example = "123456") @NotBlank(message = "OTP code is required") String code) {
}
