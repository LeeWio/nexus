package space.nebula.nexus.payload.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import space.nebula.nexus.enums.MomentVisibility;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record MomentResponse(Long id, String content, String stockSymbol, Long likesCount, Long commentsCount,
		MomentVisibility visibility, String authorName, String authorAvatar, List<MomentImageResponse> images,
		List<MomentTopicResponse> topics,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime createdAt,
		@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC") LocalDateTime updatedAt,
		MomentXSyncResponse xSync) implements Serializable {
}
