package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KanbanItemMoveRequest {

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Target column ID is required")
    private Long targetColumnId;

    @NotNull(message = "Target order index is required")
    private Integer targetOrderIndex;
}
