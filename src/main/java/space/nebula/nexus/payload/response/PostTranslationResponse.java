package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Localized post fields")
public record PostTranslationResponse(String locale, String title, String summary, String content, String autoSummary,
		String toc) {
}
