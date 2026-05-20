package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Unified results for Command+K quick search")
public record QuickSearchResponse(@Schema(description = "Matching blog posts") List<SearchResultItem> posts,

		@Schema(description = "Matching categories") List<SearchResultItem> categories,

		@Schema(description = "Matching tags") List<SearchResultItem> tags) {
	public record SearchResultItem(@Schema(description = "Unique identifier") String id,

			@Schema(description = "Display title or name") String title,

			@Schema(description = "URL path for navigation") String path) {
	}
}
