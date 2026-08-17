package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import space.nebula.nexus.enums.MomentVisibility;

import java.util.List;

@Schema(description = "Request to create or update a micro-blog 'Moment'")
public record MomentRequest(
		@Schema(description = "Markdown or text content of the moment", example = "Just finished a marathon session of coding! #productive") @NotBlank(message = "Content cannot be blank") @Size(min = 1, max = 2000, message = "Content must be between 1 and 2000 characters") String content,

		@Schema(description = "The visibility level of this moment", example = "public") @NotNull(message = "Visibility status is required") MomentVisibility visibility,

		@Schema(description = "Ordered uploaded images. Omit or send an empty list for a text-only Moment.") @Size(max = 9, message = "A moment can contain at most 9 images") List<@Valid MomentImageRequest> images) {
}
