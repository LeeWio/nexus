package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Compact post representation used by discovery surfaces.
 */
@Schema(description = "Compact post information for discovery surfaces")
public record PostDigestResponse(@Schema(description = "Post ID") Long id,
		@Schema(description = "Post title") String title, @Schema(description = "Post slug") String slug,
		@Schema(description = "Cover image URL") String coverImage,
		@Schema(description = "Short post summary") String summary,
		@Schema(description = "Author display name") String authorName,
		@Schema(description = "Author avatar URL") String authorAvatar,
		@Schema(description = "Post category") CategoryResponse category,
		@Schema(description = "Total view count") Long views, @Schema(description = "Total like count") Long likesCount,
		@Schema(description = "Total approved top-level comment count") Long commentsCount,
		@Schema(description = "Publication time") LocalDateTime publishedAt) implements Serializable {
	private static final long serialVersionUID = 1L;
}
