package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 50)
    String name,

    @NotBlank(message = "Slug cannot be empty")
    @Size(max = 50)
    String slug,

    @Size(max = 200)
    String description
) {}
