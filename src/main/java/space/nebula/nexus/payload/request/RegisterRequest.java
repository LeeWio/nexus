package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Register request payload using Java 21 Record.
 */
public record RegisterRequest(
    @NotBlank(message = "Username cannot be empty")
    @Size(min = 3, max = 20)
    String username,

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    @Size(max = 50)
    String email,

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, max = 40)
    String password
) {}
