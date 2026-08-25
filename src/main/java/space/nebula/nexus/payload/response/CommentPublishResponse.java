package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.CommentStatus;

/** Server-authoritative result for a comment submission. */
@Schema(description = "Result of publishing a comment")
public record CommentPublishResponse(
		@Schema(description = "Created comment ID") Long id,
		@Schema(description = "Persisted moderation status") CommentStatus status) {
}
