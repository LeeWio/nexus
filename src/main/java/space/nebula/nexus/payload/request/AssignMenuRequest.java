package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Request to assign multiple menus to a role")
public record AssignMenuRequest(
		@Schema(description = "List of menu IDs to be assigned", example = "[1, 2, 3]") @NotEmpty(message = "Menu IDs cannot be empty") List<Long> menuIds) {
}
