package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Lightweight public series data for homepage and discovery surfaces. */
@Schema(description = "Compact published series information for discovery surfaces")
public record SeriesSummaryResponse(@Schema(description = "Series ID") Long id,
		@Schema(description = "Series name") String name, @Schema(description = "Series slug") String slug,
		@Schema(description = "Series description") String description,
		@Schema(description = "Series cover image URL") String coverImage,
		@Schema(description = "Number of published posts") Integer postsCount,
		@Schema(description = "Series creation time") LocalDateTime createdAt) implements Serializable {
	private static final long serialVersionUID = 1L;
}
