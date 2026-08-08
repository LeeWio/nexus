package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for creating or updating a Kanban task checklist item")
public record KanbanChecklistItemRequest(
		@Schema(description = "Checklist item text", example = "Write regression tests") @NotBlank(message = "Checklist item title is required") @Size(max = 255, message = "Checklist item title must not exceed 255 characters") String title,
		@Schema(description = "Whether this checklist item is complete", example = "false") @NotNull(message = "Completed status is required") Boolean completed,
		@Schema(description = "0-based insertion position; omitted to append", example = "0") @PositiveOrZero(message = "Order index cannot be negative") Integer orderIndex) {
}
