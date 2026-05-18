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
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Schema(description = "Detailed task description")
    private String content;

    @Schema(description = "Task priority level", example = "HIGH")
    private KanbanPriority priority;

    @Schema(description = "ID of the column this task belongs to", example = "1")
    @NotNull(message = "Column ID is required")
    private Long columnId;

    @Schema(description = "Display order within the column")
    private Integer orderIndex;

    @Schema(description = "Scheduled reminder time")
    private LocalDateTime reminderAt;

    @Schema(description = "IDs of tags to assign to this task")
    private Set<Long> tagIds;
}
