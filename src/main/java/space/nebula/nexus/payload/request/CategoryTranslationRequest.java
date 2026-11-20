package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryTranslationRequest(@NotBlank @Size(max = 50) String name, @Size(max = 200) String description) {
}
