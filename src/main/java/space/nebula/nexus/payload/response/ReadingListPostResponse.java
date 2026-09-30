package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Post entry in the current user's later-reading queue.
 */
@Schema(description = "Post queued for later reading")
public record ReadingListPostResponse(PostDigestResponse post, LocalDateTime addedAt) {
}
