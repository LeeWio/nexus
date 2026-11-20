package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Localized post digest fields")
public record PostDigestTranslationResponse(String locale, String title, String summary) {
}
