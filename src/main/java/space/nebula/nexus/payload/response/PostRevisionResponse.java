package space.nebula.nexus.payload.response;

import lombok.Builder;
import space.nebula.nexus.enums.PostContentType;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
public record PostRevisionResponse(Long id, Long postId, String title, String summary, String content,
		PostContentType contentType, Integer versionNumber, String createdBy, String changeType, String changeSummary,
		String contentHash, LocalDateTime createdAt) implements Serializable {
}
