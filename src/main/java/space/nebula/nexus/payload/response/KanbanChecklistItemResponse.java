package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Kanban task checklist item")
public record KanbanChecklistItemResponse(@Schema(description = "Checklist item ID") Long id,
		@Schema(description = "Parent task ID") Long taskId, @Schema(description = "Checklist item text") String title,
		@Schema(description = "Whether the checklist item is complete") Boolean completed,
		@Schema(description = "Display order within the task") Integer orderIndex,
		@Schema(description = "Creation time") LocalDateTime createdAt,
		@Schema(description = "Last update time") LocalDateTime updatedAt) {
}
