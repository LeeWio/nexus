package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resumable reading-history entry.
 *
 * @param post
 *            compact post information
 * @param progressPercent
 *            last recorded progress percentage
 * @param positionAnchor
 *            frontend-defined stable reading position
 * @param lastReadAt
 *            most recent reading time
 * @param completedAt
 *            time at which reading reached 100 percent
 */
@Schema(description = "Reading history entry")
public record ReadingHistoryResponse(PostDigestResponse post, Integer progressPercent, String positionAnchor,
		LocalDateTime lastReadAt, LocalDateTime completedAt) {
}
