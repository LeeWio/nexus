package space.nebula.nexus.repository.projection;

import java.time.LocalDateTime;

/** Lightweight database projection for public column index cards. */
public interface ColumnSummaryProjection {
	Long getId();

	String getName();

	String getSlug();

	String getDescription();

	String getCoverImage();

	Long getPostsCount();

	LocalDateTime getCreatedAt();
}
