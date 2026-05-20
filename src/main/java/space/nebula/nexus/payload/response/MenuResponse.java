package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "Menu details with hierarchical tree support")
public class MenuResponse {
	@Schema(description = "Menu ID")
	private Long id;

	@Schema(description = "Parent Menu ID")
	private Long parentId;

	@Schema(description = "Display name")
	private String name;

	@Schema(description = "Routing path")
	private String path;

	@Schema(description = "Permission code")
	private String permission;

	@Schema(description = "Menu type (0-Dir, 1-Menu, 2-Button)")
	private Integer type;

	@Schema(description = "Icon identifier")
	private String icon;

	@Schema(description = "Display order")
	private Integer sortOrder;

	@Schema(description = "Visibility status")
	private Boolean isVisible;

	@Schema(description = "Target scope (Public vs Admin)")
	private Boolean isPublic;

	@Schema(description = "Creation time")
	private LocalDateTime createdAt;

	@Schema(description = "Child menus in the tree")
	private List<MenuResponse> children;
}
