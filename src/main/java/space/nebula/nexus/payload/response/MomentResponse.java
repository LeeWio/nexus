package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.MomentVisibility;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record MomentResponse(Long id, String content, Long likesCount, MomentVisibility visibility,
		List<MomentImageResponse> images, List<MomentTopicResponse> topics, LocalDateTime createdAt,
		LocalDateTime updatedAt) implements Serializable {
}
