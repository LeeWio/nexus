package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank(message = "Old password is required")
    String oldPassword,
    
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 characters")
    String newPassword
) {}
