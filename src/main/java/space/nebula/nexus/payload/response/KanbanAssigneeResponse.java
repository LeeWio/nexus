package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Safe display information for a user assigned to a Kanban task")
public record KanbanAssigneeResponse(@Schema(description = "User ID") Long id,
		@Schema(description = "Account username") String username,
		@Schema(description = "Optional display nickname") String nickname,
		@Schema(description = "Avatar URL") String avatar) {
}
