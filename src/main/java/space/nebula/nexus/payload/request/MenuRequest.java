package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "System menu creation or update request")
public record MenuRequest(
		@Schema(description = "Label shown in the navigation bar", example = "Blog") @NotBlank(message = "Menu name is required") @Size(max = 50, message = "Menu name must not exceed 50 characters") String name,

		@Schema(description = "ID of the parent menu (use 0 for top-level entries)", example = "0") @NotNull(message = "Parent ID is required") Long parentId,

		@Schema(description = "Frontend routing path", example = "/posts") @Size(max = 255) String path,

		@Schema(description = "Permission identifier for access control", example = "post:view") @Size(max = 100) String permission,

		@Schema(description = "Menu type: 0 for Directory, 1 for Menu, 2 for Button", example = "1") @NotNull(message = "Menu type is required") Integer type,

		@Schema(description = "Lucide icon name or CSS class", example = "FileText") @Size(max = 100) String icon,

		@Schema(description = "Manual sorting priority (lower is earlier)", example = "10") Integer sortOrder,

		@Schema(description = "Whether the menu should be rendered in the UI", example = "true") Boolean isVisible,

		@Schema(description = "True for public website menu, false for admin dashboard", example = "false") Boolean isPublic) {
}
