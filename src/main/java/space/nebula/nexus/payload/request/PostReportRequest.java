package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Post report request")
public record PostReportRequest(
		@Schema(description = "Reason for reporting the post", example = "misleading") @NotBlank(message = "Report reason is required") @Size(max = 80, message = "Report reason must not exceed 80 characters") String reason,

		@Schema(description = "Optional report details", example = "The article contains a materially incorrect claim.") @Size(max = 500, message = "Report description must not exceed 500 characters") String description) {
}
