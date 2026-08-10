package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.PostRevisionKind;

import java.time.LocalDateTime;

/**
 * Full immutable document state for one revision.
 */
public record PostRevisionDetailResponse(Long id, Long postId, Integer versionNumber, PostRevisionKind revisionKind,
		String changeType, String changeSummary, Integer baseVersionNumber, Long parentRevisionId,
		Long sourceRevisionId, String contentHash, String snapshotHash, String createdBy, LocalDateTime createdAt,
		PostRevisionSnapshot snapshot) {
}
