package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.common.validator.annotation.Slug;

/**
 * Request body for creating or updating a category
 */
@Schema(description = "Category creation/update request")
public record CategoryRequest(
		@Schema(description = "Name of the category", example = "Backend Development") @NotBlank(message = "Name is required") @Size(max = 50) String name,

		@Schema(description = "SEO URL slug", example = "backend") @NotBlank(message = "Slug is required") @Slug @Size(max = 50) String slug,

		@Schema(description = "Brief description", example = "Posts related to Java, Spring, and MySQL") @Size(max = 200) String description,

		@Schema(description = "Icon name or URL", example = "mdi:language-java") @Size(max = 100) String icon) {
}
