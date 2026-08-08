package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change the completion state of a Kanban checklist item")
public record KanbanChecklistItemCompletionRequest(
		@Schema(description = "Whether the checklist item is complete", example = "true") @NotNull(message = "Completed status is required") Boolean completed) {
}
