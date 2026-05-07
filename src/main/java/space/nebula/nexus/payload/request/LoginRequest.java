package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request payload using Java 21 Record.
 */
public record LoginRequest(
    @NotBlank(message = "Username cannot be empty") String username,
    @NotBlank(message = "Password cannot be empty") String password
) {}
