package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A life-list category")
public record LifeListCategoryResponse(
		Long id,
		String name,
		String slug,
		int sortOrder) {
}
