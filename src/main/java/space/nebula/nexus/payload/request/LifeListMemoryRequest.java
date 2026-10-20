package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.enums.LifeListVisibility;

import java.time.LocalDate;

@Schema(description = "A memory attached to a wish. Saving one does not change the wish status.")
public record LifeListMemoryRequest(
		@Schema(description = "When the experience happened") LocalDate occurredAt,
		@NotBlank @Size(max = 8000) String content,
		@Size(max = 160) String location,
		@NotNull LifeListVisibility visibility) {
}
