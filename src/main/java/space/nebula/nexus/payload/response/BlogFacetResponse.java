package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.PostContentType;

import java.io.Serializable;
import java.util.List;

/**
 * Aggregated public blog facets for archive and browse surfaces.
 */
@Schema(description = "Aggregated blog browse facets")
public record BlogFacetResponse(@Schema(description = "Total number of published posts") long totalPublishedCount,
		@Schema(description = "Published post counts by category") List<CategoryFacet> categories,
		@Schema(description = "Published post counts by tag") List<TagFacet> tags,
		@Schema(description = "Published post counts by archive month") List<ArchiveFacet> archives,
		@Schema(description = "Published post counts by content format") List<ContentTypeFacet> contentTypes)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;

	public record CategoryFacet(Long id, String name, String slug, long count) implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	public record TagFacet(Long id, String name, String slug, long count) implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	public record ArchiveFacet(int year, int month, long count) implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	public record ContentTypeFacet(PostContentType contentType, long count) implements Serializable {
		private static final long serialVersionUID = 1L;
	}
}
