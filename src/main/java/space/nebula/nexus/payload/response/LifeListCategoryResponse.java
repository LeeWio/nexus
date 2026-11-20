package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "A life-list category")
public record LifeListCategoryResponse(Long id, String name, String slug, int sortOrder,
		Map<String, LifeListCategoryTranslationResponse> translations) {
	public LifeListCategoryResponse(Long id, String name, String slug, int sortOrder) {
		this(id, name, slug, sortOrder, Map.of());
	}
}
