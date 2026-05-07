package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRequest(
    @NotBlank(message = "Name cannot be empty")
    @Size(max = 50)
    String name,

    @NotBlank(message = "Slug cannot be empty")
    @Size(max = 50)
    String slug
) {}
