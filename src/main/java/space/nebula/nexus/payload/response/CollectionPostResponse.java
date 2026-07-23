package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Post entry stored in a personal collection.
 *
 * @param post
 *            compact post information
 * @param addedAt
 *            time at which the post was added
 */
@Schema(description = "Post stored in a personal collection")
public record CollectionPostResponse(PostDigestResponse post, LocalDateTime addedAt) {
}
