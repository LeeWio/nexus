package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Request to replace every assignee on a Kanban task")
public record KanbanTaskAssigneeRequest(
		@Schema(description = "IDs of active users assigned to the task; an empty set removes all assignees", example = "[2, 5]") @NotNull(message = "Assignee IDs are required") @Size(max = 20, message = "A task can have at most 20 assignees") Set<@NotNull(message = "Assignee ID is required") Long> assigneeIds) {
}
