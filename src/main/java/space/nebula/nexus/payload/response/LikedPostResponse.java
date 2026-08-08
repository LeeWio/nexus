package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Post entry in the current user's liked-article library.
 *
 * @param post
 *            compact post information
 * @param likedAt
 *            time at which the user liked the post
 */
@Schema(description = "Liked post entry")
public record LikedPostResponse(PostDigestResponse post, LocalDateTime likedAt) {
}
