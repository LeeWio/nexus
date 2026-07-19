package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Favorite post entry in the current user's library.
 *
 * @param post compact post information
 * @param favoritedAt time at which the post was favorited
 */
@Schema(description = "Favorite post entry")
public record FavoritePostResponse(PostDigestResponse post, LocalDateTime favoritedAt)
{
}
