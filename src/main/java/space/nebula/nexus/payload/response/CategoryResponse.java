package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Category response payload.
 */
@Builder
@Schema(description = "Category details")
public record CategoryResponse(@Schema(description = "Category ID") Long id,

		@Schema(description = "Category name") String name,

		@Schema(description = "Category slug") String slug,

		@Schema(description = "Category description") String description,

		@Schema(description = "Category icon") String icon,

		@Schema(description = "Creation time") LocalDateTime createdAt,

		@Schema(description = "Localized category fields keyed by BCP-47 locale") Map<String, CategoryTranslationResponse> translations)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;

	public CategoryResponse(Long id, String name, String slug, String description, String icon,
			LocalDateTime createdAt) {
		this(id, name, slug, description, icon, createdAt, Map.of());
	}
}
