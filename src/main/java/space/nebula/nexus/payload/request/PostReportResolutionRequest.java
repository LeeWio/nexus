package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.enums.PostReportStatus;

@Schema(description = "Administrative post report resolution")
public record PostReportResolutionRequest(
		@Schema(description = "Final report status", example = "DISMISSED") @NotNull(message = "Report status is required") PostReportStatus status,
		@Schema(description = "Optional moderation note", example = "Reviewed and no policy violation was found.") @Size(max = 500, message = "Resolution note must not exceed 500 characters") String resolutionNote) {
}
