package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request for recording a user's resumable reading position.
 *
 * @param progressPercent progress percentage from 0 through 100
 * @param positionAnchor frontend-defined stable reading position
 */
@Schema(description = "Reading progress update")
public record ReadingProgressRequest(
		@NotNull @Min(0) @Max(100) @Schema(description = "Reading progress percentage", example = "65") Integer progressPercent,
		@Size(max = 500) @Schema(description = "Stable frontend reading position") String positionAnchor)
{
}
