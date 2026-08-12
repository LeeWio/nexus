package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Public reader view of an editorial column. Article entries are deliberately
 * compact so column pages do not transfer article bodies or editorial state.
 */
@Schema(description = "Published editorial column with compact article entries")
public record ColumnResponse(@Schema(description = "Column ID") Long id,

		@Schema(description = "Column title") String name,

		@Schema(description = "URL-safe column slug") String slug,

		@Schema(description = "Column introduction") String description,

		@Schema(description = "Column cover image URL") String coverImage,

		@Schema(description = "Whether the column is publicly visible") Boolean isPublished,

		@Schema(description = "Number of published articles") Integer postsCount,

		@Schema(description = "Published article summaries, ordered for this column") List<ColumnPostResponse> posts,

		@Schema(description = "Column creation time") LocalDateTime createdAt) implements Serializable {
	private static final long serialVersionUID = 1L;
}
