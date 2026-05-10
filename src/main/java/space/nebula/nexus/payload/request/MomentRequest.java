package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MomentRequest(
    @NotBlank(message = "Content cannot be blank")
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    String content,

    @NotNull(message = "Publish status is required")
    Boolean isPublished
) {}
