package space.nebula.nexus.payload.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import space.nebula.nexus.enums.CommentStatus;

import java.util.List;

@Schema(description = "Request for applying one moderation decision to multiple comments")
public record BatchModerateCommentRequest(
		@Schema(description = "List of comment identifiers to moderate", example = "[10, 11, 12]") @NotEmpty(message = "Comment IDs are required") List<Long> ids,

		@Schema(description = "Target moderation status", example = "APPROVED") @NotNull(message = "Moderation status is required") CommentStatus status) {
}
