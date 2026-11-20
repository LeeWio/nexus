package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LifeListCategoryTranslationRequest(@NotBlank @Size(max = 80) String name) {
}
