package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create or update a micro-blog 'Moment'")
public record MomentRequest(
		@Schema(description = "Markdown or text content of the moment", example = "Just finished a marathon session of coding! #productive") @NotBlank(message = "Content cannot be blank") @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters") String content,

		@Schema(description = "Whether this moment is visible to the public", example = "true") @NotNull(message = "Publish status is required") Boolean isPublished) {
}
