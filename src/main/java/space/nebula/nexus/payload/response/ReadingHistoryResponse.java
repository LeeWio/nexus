package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resumable reading-history entry.
 *
 * @param post
 *            compact post information
 * @param currentProgressPercent
 *            progress at the latest observed position
 * @param furthestProgressPercent
 *            furthest progress reached by the reader
 * @param positionAnchor
 *            frontend-defined stable reading position
 * @param lastReadAt
 *            most recent reading time
 * @param completedAt
 *            time at which reading reached 100 percent
 */
@Schema(description = "Reading history entry")
public record ReadingHistoryResponse(PostDigestResponse post, Integer currentProgressPercent,
		Integer furthestProgressPercent, String positionAnchor, LocalDateTime lastReadAt, LocalDateTime completedAt) {
}
