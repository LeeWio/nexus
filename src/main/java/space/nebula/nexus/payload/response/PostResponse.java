package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Post response payload using Java 21 Record.
 */
@Builder(toBuilder = true)
@Schema(description = "Post details and interaction data")
public record PostResponse(@Schema(description = "Post ID") Long id,

		@Schema(description = "Post title") String title,

		@Schema(description = "Post slug") String slug,

		@Schema(description = "Post cover image URL") String coverImage,

		@Schema(description = "Post summary") String summary,

		@Schema(description = "Post full content") String content,

		@Schema(description = "Format of the content (JSON, MDX)") PostContentType contentType,

		@Schema(description = "Current status") PostStatus status,

		@Schema(description = "Is featured post") Boolean isFeatured,

		@Schema(description = "Total view count") Long views,

		@Schema(description = "Total like count") Long likesCount,

		@Schema(description = "Total favorite count") Long favoritesCount,

		@Schema(description = "Whether current user liked this post") Boolean isLiked,

		@Schema(description = "Whether current user favorited this post") Boolean isFavorited,

		@Schema(description = "Author nickname or username") String authorName,

		@Schema(description = "Author avatar URL") String authorAvatar,

		@Schema(description = "Category details") CategoryResponse category,

		@Schema(description = "Series details") SeriesResponse series,

		@Schema(description = "Ordering index within the series") Integer seriesOrder,

		@Schema(description = "ID of the parent post") Long parentId,

		@Schema(description = "Hierarchy path") String path,

		@Schema(description = "Set of associated tags") Set<TagResponse> tags,

		@Schema(description = "Creation time") LocalDateTime createdAt,

		@Schema(description = "Last update time") LocalDateTime updatedAt,

		@Schema(description = "Breadcrumb trail for navigation") java.util.List<Breadcrumb> breadcrumbs,

		@Schema(description = "SEO and OpenGraph metadata") SeoMetadata seo,

		@Schema(description = "Navigation links to adjacent posts") Navigation navigation) implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * SEO Metadata for social sharing and search engines.
	 */
	public record SeoMetadata(
			String ogTitle,
			String ogDescription,
			String ogImage,
			String ogType,
			String ogUrl,
			String twitterCard,
			String canonicalUrl
	) implements Serializable {}

	/**
	 * Compact representation of a post for breadcrumbs.
	 */
	public record Breadcrumb(Long id, String title, String slug) implements Serializable {
	}

	/**
	 * Navigation links for sequential reading.
	 */
	public record Navigation(Neighbor prev, Neighbor next) implements Serializable {
		public record Neighbor(String title, String slug) implements Serializable {
		}
	}
}
