package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.enums.PostStatus;

import java.util.Set;

/**
 * Post request payload using Java 21 Record.
 */
public record PostRequest(
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 200)
    String title,

    @Size(max = 200)
    String slug,

    String coverImage,

    @Size(max = 500)
    String summary,

    @NotBlank(message = "Content cannot be empty")
    String content,

    @NotNull(message = "Status is required")
    PostStatus status,

    Boolean isFeatured,

    Long categoryId,

    Set<Long> tagIds
) {
    public PostRequest {
        if (isFeatured == null) isFeatured = false;
    }
}
