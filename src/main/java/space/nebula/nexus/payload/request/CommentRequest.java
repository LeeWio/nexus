package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for submitting a comment.
 */
public record CommentRequest(
    @NotBlank(message = "Comment content cannot be empty")
    @Size(max = 1000, message = "Comment content too long")
    String content,

    Long postId,

    Long parentId
) {}
