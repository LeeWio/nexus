package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Moment comment submission request")
public record MomentCommentRequest(
		@Schema(description = "Comment content", example = "Nice moment!") @NotBlank(message = "Comment content is required") @Size(max = 1000, message = "Comment content must not exceed 1000 characters") String content,
		@Schema(description = "Target moment ID", example = "12") @NotNull(message = "Moment ID is required") Long momentId,
		@Schema(description = "Parent comment ID for replies") Long parentId) {
}
