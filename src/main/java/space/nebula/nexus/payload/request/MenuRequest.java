package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Menu creation/update request")
public record MenuRequest(
    @Schema(description = "Display name", example = "Blog")
    @NotBlank(message = "Menu name is required")
    String name,
    
    @Schema(description = "ID of the parent menu (0 for top-level)")
    Long parentId,
    
    @Schema(description = "Routing path", example = "/posts")
    String path,
    
    @Schema(description = "Permission code", example = "post:view")
    String permission,
    
    @Schema(description = "Menu type (0-Dir, 1-Menu, 2-Button)")
    @NotNull(message = "Menu type is required")
    Integer type,
    
    @Schema(description = "Icon identifier")
    String icon,
    
    @Schema(description = "Display order")
    Integer sortOrder,

    @Schema(description = "Whether the menu is visible in navigation")
    Boolean isVisible,

    @Schema(description = "Whether the menu is for the public website (true) or admin panel (false)")
    Boolean isPublic
) {}
