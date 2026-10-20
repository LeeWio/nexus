package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.LifeListVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "A recorded experience for one wish")
public record LifeListMemoryResponse(
		Long id,
		Long itemId,
		LocalDate occurredAt,
		String content,
		String location,
		LifeListVisibility visibility,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {
}
