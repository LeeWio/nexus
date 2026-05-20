package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Kanban column with its items")
public class KanbanColumnResponse {
	@Schema(description = "Column ID")
	private Long id;

	@Schema(description = "Column name")
	private String name;

	@Schema(description = "Hex color code")
	private String color;

	@Schema(description = "Display order index")
	private Integer orderIndex;

	@Schema(description = "List of tasks in this column")
	private List<KanbanItemResponse> items;

	@Schema(description = "Creation time")
	private LocalDateTime createdAt;

	@Schema(description = "Last update time")
	private LocalDateTime updatedAt;
}
