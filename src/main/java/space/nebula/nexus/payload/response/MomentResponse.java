package space.nebula.nexus.payload.response;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record MomentResponse(Long id, String content, Long likesCount, Boolean isPublished,
		List<MomentImageResponse> images, LocalDateTime createdAt, LocalDateTime updatedAt) implements Serializable {
}
