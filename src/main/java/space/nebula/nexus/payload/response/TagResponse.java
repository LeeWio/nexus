package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Tag response payload.
 */
@Builder
@Schema(description = "Tag details")
public record TagResponse(@Schema(description = "Tag ID") Long id,

		@Schema(description = "Tag name") String name,

		@Schema(description = "Tag slug") String slug,

		@Schema(description = "Creation time") LocalDateTime createdAt,

		@Schema(description = "Localized tag fields keyed by BCP-47 locale") Map<String, TagTranslationResponse> translations)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;

	public TagResponse(Long id, String name, String slug, LocalDateTime createdAt) {
		this(id, name, slug, createdAt, Map.of());
	}
}
