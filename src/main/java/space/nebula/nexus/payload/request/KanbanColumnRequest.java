package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request for creating or updating a Kanban column")
public class KanbanColumnRequest {

	@Schema(description = "Display name of the column", example = "In Progress")
	@NotBlank(message = "Column name is required")
	@Size(max = 100, message = "Column name must be less than 100 characters")
	private String name;

	@Schema(description = "Hex color code for the column UI", example = "#3498db")
	@Size(max = 50, message = "Color code must be less than 50 characters")
	private String color;

	@Schema(description = "Manual display order index", example = "0")
	private Integer orderIndex;
}
