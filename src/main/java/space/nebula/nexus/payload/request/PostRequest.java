package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.common.validator.annotation.Slug;
import space.nebula.nexus.enums.PostStatus;

import java.util.Set;

/**
 * Post request payload using Java 21 Record.
 */
@Schema(description = "Request body for creating or updating a blog post")
public record PostRequest(
    @Schema(description = "Title of the post", example = "Getting Started with Spring Boot 3")
    @NotBlank(message = "Title cannot be empty")
    @Size(max = 200)
    String title,

    @Schema(description = "SEO friendly URL slug", example = "spring-boot-3-intro")
    @Slug
    @Size(max = 200)
    String slug,

    @Schema(description = "Full URL to the cover image")
    String coverImage,

    @Schema(description = "Short summary/excerpt of the post", example = "A comprehensive guide to the latest Spring Boot version.")
    @Size(max = 500)
    String summary,

    @Schema(description = "Main content in Markdown or HTML")
    @NotBlank(message = "Content cannot be empty")
    String content,

    @Schema(description = "Publishing status", example = "PUBLISHED")
    @NotNull(message = "Status is required")
    PostStatus status,

    @Schema(description = "Whether the post is highlighted/pinned", example = "false")
    Boolean isFeatured,

    @Schema(description = "ID of the category this post belongs to", example = "1")
    Long categoryId,

    @Schema(description = "ID of the series this post belongs to", example = "1")
    Long seriesId,

    @Schema(description = "Ordering index within the series", example = "1")
    Integer seriesOrder,

    @Schema(description = "Set of tag IDs to associate with this post", example = "[1, 2, 5]")
    Set<Long> tagIds
) {
    public PostRequest {
        if (isFeatured == null) isFeatured = false;
    }
}
