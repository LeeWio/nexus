package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@Schema(description = "Request to move a Kanban item to a new position or column")
public class KanbanItemMoveRequest {

	@Schema(description = "ID of the task item to be moved", example = "101")
	@NotNull(message = "Item ID is required")
	private Long itemId;

	@Schema(description = "ID of the column where the item should be placed", example = "2")
	@NotNull(message = "Target column ID is required")
	private Long targetColumnId;

	@Schema(description = "The new 0-based index position within the target column", example = "0")
	@NotNull(message = "Target order index is required")
	@PositiveOrZero(message = "Target order index cannot be negative")
	private Integer targetOrderIndex;
}
