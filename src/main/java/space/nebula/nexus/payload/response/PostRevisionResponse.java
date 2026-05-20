package space.nebula.nexus.payload.response;

import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
public record PostRevisionResponse(Long id, Long postId, String title, String summary, String content,
		Integer versionNumber, String createdBy, LocalDateTime createdAt) implements Serializable {
}
