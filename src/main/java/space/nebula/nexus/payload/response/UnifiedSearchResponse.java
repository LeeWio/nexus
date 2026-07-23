package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Professional unified search results for Command+K and global search")
public class UnifiedSearchResponse {

	@Schema(description = "Grouped results for better UI organization")
	private List<SearchGroup> groups;

	@Schema(description = "Total hits across all categories")
	private long totalHits;

	@Schema(description = "Time taken in milliseconds")
	private long processingTimeMs;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SearchGroup {
		@Schema(description = "Internal type identifier (POST, CATEGORY, TAG, PROJECT, MOMENT, ACTION)")
		private String type;

		@Schema(description = "Display label for the group")
		private String label;

		@Schema(description = "Display priority for sorting groups in the UI (lower is higher priority)")
		private int priority;

		@Schema(description = "List of matches in this group")
		private List<SearchResultItem> items;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class SearchResultItem {
		@Schema(description = "Unique identifier with prefix (e.g., post:1)")
		private String id;

		@Schema(description = "Primary display title")
		private String title;

		@Schema(description = "Secondary descriptive text or professional snippet")
		private String subtitle;

		@Schema(description = "Longer description for detailed views or tooltips")
		private String description;

		@Schema(description = "Absolute navigation path for Next.js (e.g., /post/slug)")
		private String url;

		@Schema(description = "Optional icon identifier (Lucide icon name)")
		private String icon;

		@Schema(description = "Hex or Tailwind color for the icon (e.g., #3b82f6)")
		private String iconColor;

		@Schema(description = "Type of the item (POST, CATEGORY, TAG, etc.)")
		private String type;

		@Schema(description = "Keyboard shortcut for kbar (e.g., ['G', 'H'])")
		private List<String> shortcut;

		@Schema(description = "Relevance score for sorting")
		private Double score;

		@Schema(description = "Additional context (e.g., date, count, status)")
		private Map<String, Object> metadata;
	}
}
