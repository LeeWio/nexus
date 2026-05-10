package space.nebula.nexus.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuRequest(
    @NotBlank(message = "Menu name is required")
    String name,
    
    Long parentId,
    
    String path,
    
    String permission,
    
    @NotNull(message = "Menu type is required")
    Integer type,
    
    String icon,
    
    Integer sortOrder
) {}
