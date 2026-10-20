package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import space.nebula.nexus.enums.LifeListStatus;

import java.time.LocalDate;

@Schema(description = "Change a wish status without rewriting its description")
public record LifeListStatusRequest(
		@NotNull LifeListStatus status,
		@Schema(description = "Completion date. Defaults to today when completing and omitted.") LocalDate completedAt) {
}
