package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to change user password")
public record PasswordChangeRequest(
    @Schema(description = "Current password", example = "OldP@ssw0rd123!") 
    @NotBlank(message = "Current password is required")
    String currentPassword,

    @Schema(description = "New password", example = "NewP@ssw0rd123!") 
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    String newPassword
) {}
