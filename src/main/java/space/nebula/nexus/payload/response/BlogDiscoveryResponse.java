package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;

/**
 * Curated public blog content grouped for a discovery-oriented layout.
 */
@Schema(description = "Curated content for the public blog discovery experience")
public record BlogDiscoveryResponse(@Schema(description = "Primary editorial post") PostDigestResponse spotlight,
		@Schema(description = "Curated prominent posts selected by editorial and engagement score") List<PostDigestResponse> curated,
		@Schema(description = "Recently published posts") List<PostDigestResponse> latest,
		@Schema(description = "Posts with the strongest recent discovery signals") List<PostDigestResponse> trending,
		@Schema(description = "Most-read posts not already shown") List<PostDigestResponse> mostRead,
		@Schema(description = "Published series selected for the discovery page") List<SeriesSummaryResponse> series,
		@Schema(description = "High-signal post groups by category") List<CategoryGroup> categoryGroups,
		@Schema(description = "UTC time when this discovery snapshot was generated") Instant generatedAt,
		@Schema(description = "Ranking algorithm version used to generate this snapshot") String algorithmVersion)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;

	public BlogDiscoveryResponse(PostDigestResponse spotlight, List<PostDigestResponse> latest,
			List<PostDigestResponse> mostRead) {
		this(spotlight, List.of(), latest, List.of(), mostRead, List.of(), List.of(), null, null);
	}

	public BlogDiscoveryResponse(PostDigestResponse spotlight, List<PostDigestResponse> curated,
			List<PostDigestResponse> latest, List<PostDigestResponse> mostRead, List<CategoryGroup> categoryGroups) {
		this(spotlight, curated, latest, List.of(), mostRead, List.of(), categoryGroups, null, null);
	}

	public BlogDiscoveryResponse {
		curated = List.copyOf(curated);
		latest = List.copyOf(latest);
		trending = List.copyOf(trending);
		mostRead = List.copyOf(mostRead);
		series = List.copyOf(series);
		categoryGroups = List.copyOf(categoryGroups);
	}

	@Schema(description = "Prominent content group for one category")
	public record CategoryGroup(@Schema(description = "Category represented by this group") CategoryResponse category,
			@Schema(description = "Primary post for this category") PostDigestResponse heroPost,
			@Schema(description = "Supporting posts for this category") List<PostDigestResponse> supportingPosts,
			@Schema(description = "Posts selected for this category") List<PostDigestResponse> posts,
			@Schema(description = "Number of selected posts in this category group") int totalPublishedCount,
			@Schema(description = "Latest publication time among selected posts") LocalDateTime latestPublishedAt,
			@Schema(description = "Aggregate ranking score used to order groups") double score)
			implements
				Serializable {
		public CategoryGroup(CategoryResponse category, List<PostDigestResponse> posts, double score) {
			this(category, posts.isEmpty() ? null : posts.getFirst(),
					posts.size() <= 1 ? List.of() : posts.subList(1, posts.size()), posts, posts.size(),
					posts.stream().map(PostDigestResponse::publishedAt).filter(java.util.Objects::nonNull)
							.max(LocalDateTime::compareTo).orElse(null),
					score);
		}

		public CategoryGroup {
			supportingPosts = List.copyOf(supportingPosts);
			posts = List.copyOf(posts);
		}
	}
}
