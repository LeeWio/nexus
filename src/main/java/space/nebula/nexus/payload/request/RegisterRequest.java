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
    @Schema(description = "Desired username", example = "john_doe", minLength = 3, maxLength = 20)
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 20)
    String username,

    @Schema(description = "User email address", example = "john@example.com")
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    @Size(max = 50)
    String email,

    @Schema(description = "User password (must contain digit and special char)", example = "SecurePass123!")
    @NotBlank(message = "Password cannot be empty")
    @Password
    String password
) {}
