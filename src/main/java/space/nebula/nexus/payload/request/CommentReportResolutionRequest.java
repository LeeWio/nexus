package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import space.nebula.nexus.enums.CommentReportStatus;

@Schema(description = "Administrative comment report resolution")
public record CommentReportResolutionRequest(
		@Schema(description = "Final report status", example = "DISMISSED") @NotNull(message = "Report status is required") CommentReportStatus status,
		@Schema(description = "Optional moderation note", example = "Reviewed; no policy violation.") @Size(max = 500, message = "Resolution note must not exceed 500 characters") String resolutionNote) {
}
