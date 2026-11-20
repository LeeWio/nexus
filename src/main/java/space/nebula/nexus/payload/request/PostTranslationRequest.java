package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostTranslationRequest(@NotBlank @Size(max = 200) String title, @Size(max = 500) String summary,
		String content, @Size(max = 500) String autoSummary, String toc) {
}
