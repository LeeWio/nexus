package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.common.validator.annotation.Slug;

@Schema(description = "Post Series creation/update request")
public record SeriesRequest(
		@Schema(description = "Name of the series", example = "Spring Boot Deep Dive") @NotBlank(message = "Series name is required") @Size(max = 100) String name,

		@Schema(description = "SEO friendly slug", example = "spring-boot-tutorial") @NotBlank(message = "Slug is required") @Slug @Size(max = 100) String slug,

		@Schema(description = "Detailed description", example = "A collection of advanced tutorials.") @Size(max = 500) String description,

		@Schema(description = "Cover image URL") String coverImage,

		@Schema(description = "Whether the series is publicly visible", example = "true") Boolean isPublished) {
	public SeriesRequest {
		if (isPublished == null)
			isPublished = true;
	}
}
