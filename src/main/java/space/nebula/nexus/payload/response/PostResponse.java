package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import space.nebula.nexus.enums.PostStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Post response payload using Java 21 Record.
 */
@Builder(toBuilder = true)
@Schema(description = "Post details and interaction data")
public record PostResponse(
    @Schema(description = "Post ID")
    Long id,
    
    @Schema(description = "Post title")
    String title,
    
    @Schema(description = "Post slug")
    String slug,
    
    @Schema(description = "Post cover image URL")
    String coverImage,
    
    @Schema(description = "Post summary")
    String summary,
    
    @Schema(description = "Post full content")
    String content,
    
    @Schema(description = "Current status")
    PostStatus status,
    
    @Schema(description = "Is featured post")
    Boolean isFeatured,
    
    @Schema(description = "Total view count")
    Long views,
    
    @Schema(description = "Total like count")
    Long likesCount,
    
    @Schema(description = "Total favorite count")
    Long favoritesCount,
    
    @Schema(description = "Whether current user liked this post")
    Boolean isLiked,
    
    @Schema(description = "Whether current user favorited this post")
    Boolean isFavorited,
    
    @Schema(description = "Author nickname or username")
    String authorName,
    
    @Schema(description = "Category details")
    CategoryResponse category,

    @Schema(description = "Series details")
    SeriesResponse series,

    @Schema(description = "Ordering index within the series")
    Integer seriesOrder,

    @Schema(description = "Associated tags")
    Set<TagResponse> tags,
    
    @Schema(description = "Creation time")
    LocalDateTime createdAt,
    
    @Schema(description = "Last update time")
    LocalDateTime updatedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
