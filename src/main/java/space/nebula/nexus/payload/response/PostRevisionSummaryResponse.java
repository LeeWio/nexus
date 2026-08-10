package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.PostRevisionKind;

import java.time.LocalDateTime;

/**
 * Lightweight revision metadata for history timelines. Content is retrieved
 * separately to avoid loading large article bodies for every revision.
 */
public record PostRevisionSummaryResponse(Long id, Long postId, String title, Integer versionNumber,
		PostRevisionKind revisionKind, String changeType, String changeSummary, Integer baseVersionNumber,
		Long parentRevisionId, Long sourceRevisionId, String contentHash, String snapshotHash, String createdBy,
		LocalDateTime createdAt) {
}
