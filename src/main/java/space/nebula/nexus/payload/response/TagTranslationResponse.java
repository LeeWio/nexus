package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Localized tag fields")
public record TagTranslationResponse(String locale, String name) {
}
