package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "An uploaded image attached to a Moment")
public record MomentImageRequest(
		@Schema(description = "Uploaded file asset ID", example = "42") @NotNull @Positive Long fileId,
		@Schema(description = "Accessible description of the image", example = "Sunset over the river after work") @NotBlank @Size(max = 300) String altText) {
}
