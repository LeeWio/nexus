package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.MomentXSyncStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

public record MomentXSyncResponse(MomentXSyncStatus status, String xPostId, String url, String error, Integer attempts,
		LocalDateTime postedAt) implements Serializable {
}
