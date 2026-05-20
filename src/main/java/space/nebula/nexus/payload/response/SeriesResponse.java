package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "Post Series details")
public record SeriesResponse(@Schema(description = "Series ID") Long id,

		@Schema(description = "Series name") String name,

		@Schema(description = "Series slug") String slug,

		@Schema(description = "Series description") String description,

		@Schema(description = "Series cover image URL") String coverImage,

		@Schema(description = "Whether the series is published") Boolean isPublished,

		@Schema(description = "Total number of posts in this series") Integer postsCount,

		@Schema(description = "List of posts in this series (if requested)") List<PostResponse> posts,

		@Schema(description = "Creation time") LocalDateTime createdAt) implements Serializable {
	private static final long serialVersionUID = 1L;
}
