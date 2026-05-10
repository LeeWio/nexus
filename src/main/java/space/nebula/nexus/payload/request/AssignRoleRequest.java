package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssignRoleRequest(
    @NotEmpty(message = "Role IDs cannot be empty")
    List<Long> roleIds
) {}
