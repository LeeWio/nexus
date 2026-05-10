package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FriendLinkRequest(
    @NotBlank(message = "Site name is required")
    @Size(max = 100, message = "Site name must not exceed 100 characters")
    String name,

    @NotBlank(message = "URL is required")
    @Size(max = 255, message = "URL must not exceed 255 characters")
    String url,

    String avatar,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    Integer sortOrder,

    @NotNull(message = "Publish status is required")
    Boolean isPublished
) {}
