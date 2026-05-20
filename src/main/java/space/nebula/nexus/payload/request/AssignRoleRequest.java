package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Request to assign multiple roles to a user")
public record AssignRoleRequest(
		@Schema(description = "List of role IDs to be assigned", example = "[1, 2]") @NotEmpty(message = "Role IDs cannot be empty") List<Long> roleIds) {
}
