package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import space.nebula.nexus.enums.KanbanPriority;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Schema(description = "Kanban task item details")
public class KanbanItemResponse {
	@Schema(description = "Task ID")
	private Long id;

	@Schema(description = "Task title")
	private String title;

	@Schema(description = "Detailed description")
	private String content;

	@Schema(description = "Priority level")
	private KanbanPriority priority;

	@Schema(description = "Display order within column")
	private Integer orderIndex;

	@Schema(description = "Parent column ID")
	private Long columnId;

	@Schema(description = "Scheduled reminder time")
	private LocalDateTime reminderAt;

	@Schema(description = "List of associated tags")
	private Set<TagResponse> tags;

	@Schema(description = "Creation time")
	private LocalDateTime createdAt;

	@Schema(description = "Last update time")
	private LocalDateTime updatedAt;
}
