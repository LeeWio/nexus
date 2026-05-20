package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create or update a security role")
public record RoleRequest(
		@Schema(description = "Display name of the role", example = "Administrator") @NotBlank(message = "Name cannot be empty") @Size(min = 1, max = 50, message = "Name must be between 1 and 50 characters") String name,

		@Schema(description = "Unique internal code for the role", example = "ROLE_ADMIN") @NotBlank(message = "Code cannot be empty") @Size(min = 1, max = 50, message = "Code must be between 1 and 50 characters") String code,

		@Schema(description = "Brief explanation of role permissions") @Size(max = 200) String description) {
}
