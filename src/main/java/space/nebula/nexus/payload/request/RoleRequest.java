package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequest(
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 50)
    String name,

    @NotBlank(message = "Code cannot be empty")
    @Size(max = 50)
    String code,

    @Size(max = 200)
    String description
) {}
