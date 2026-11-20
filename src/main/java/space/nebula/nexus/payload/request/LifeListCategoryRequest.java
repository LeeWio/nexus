package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

@Schema(description = "Create or rename a life-list category")
public record LifeListCategoryRequest(@Schema(example = "旅行与探索") @NotBlank @Size(max = 80) String name,
		@Schema(example = "travel") @Size(max = 80) String slug, @Schema(example = "10") Integer sortOrder,
		Map<String, LifeListCategoryTranslationRequest> translations) {
	public LifeListCategoryRequest(String name, String slug, Integer sortOrder) {
		this(name, slug, sortOrder, null);
	}
}
