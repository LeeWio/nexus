package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to send a One-Time Password to an email")
public record OtpSendRequest(
    @Schema(description = "User's email address", example = "user@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email
) {}
