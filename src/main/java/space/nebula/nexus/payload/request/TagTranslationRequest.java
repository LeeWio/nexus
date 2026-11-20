package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagTranslationRequest(@NotBlank @Size(max = 50) String name) {
}
