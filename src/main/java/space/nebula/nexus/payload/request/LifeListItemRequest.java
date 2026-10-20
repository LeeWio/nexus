package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.enums.LifeListVisibility;

import java.time.LocalDate;

@Schema(description = "Create or update a life-list wish")
public record LifeListItemRequest(
		@Schema(example = "Walk the Kumano Kodo") @NotBlank @Size(max = 160) String title,
		@Schema(description = "Optional longer description") @Size(max = 8000) String description,
		@Schema(description = "Why this wish matters") @Size(max = 4000) String motivation,
		@Schema(description = "Category id. Omit to leave uncategorized.") Long categoryId,
		@NotNull LifeListStatus status,
		@NotNull LifeListVisibility visibility,
		@Schema(description = "Display order. Number is assigned by the server and is not this field.") Integer sortOrder,
		@Schema(description = "Required when status is completed. Ignored otherwise.") LocalDate completedAt) {
}
