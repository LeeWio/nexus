package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssignMenuRequest(
    @NotEmpty(message = "Menu IDs cannot be empty")
    List<Long> menuIds
) {}
