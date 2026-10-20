package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.enums.LifeListVisibility;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "A life-list wish. Public responses omit private memories.")
public record LifeListItemResponse(Long id, int number, String title, String description, String motivation,
		LifeListCategoryResponse category, LifeListStatus status, LifeListVisibility visibility, int sortOrder,
		LocalDate completedAt, LocalDateTime archivedAt, long memoryCount, List<LifeListMemoryResponse> memories,
		LocalDateTime createdAt, LocalDateTime updatedAt) {
}
