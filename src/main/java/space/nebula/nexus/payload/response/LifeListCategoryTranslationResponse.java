package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Localized life-list category fields")
public record LifeListCategoryTranslationResponse(String locale, String name) {
}
