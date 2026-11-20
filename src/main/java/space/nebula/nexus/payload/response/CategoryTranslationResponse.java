package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Localized category fields")
public record CategoryTranslationResponse(String locale, String name, String description) {
}
