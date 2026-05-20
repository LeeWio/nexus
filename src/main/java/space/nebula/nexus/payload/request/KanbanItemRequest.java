package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import space.nebula.nexus.enums.KanbanPriority;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Schema(description = "Request for creating or updating a Kanban task item")
public class KanbanItemRequest {

	@Schema(description = "Title of the task", example = "Fix login bug")
	@NotBlank(message = "Title is required")
	@Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
	private String title;

	@Schema(description = "Detailed task description in Markdown or plain text")
	@Size(max = 2000, message = "Content must not exceed 2000 characters")
	private String content;

	@Schema(description = "Task priority level", example = "HIGH")
	@NotNull(message = "Priority is required")
	private KanbanPriority priority;

	@Schema(description = "ID of the column this task belongs to", example = "1")
	@NotNull(message = "Column ID is required")
	private Long columnId;

	@Schema(description = "Display order within the column", example = "0")
	private Integer orderIndex;

	@Schema(description = "Scheduled reminder time (ISO 8601)", example = "2026-05-20T10:00:00")
	private LocalDateTime reminderAt;

	@Schema(description = "IDs of tags to assign to this task", example = "[1, 3]")
	private Set<Long> tagIds;
}
