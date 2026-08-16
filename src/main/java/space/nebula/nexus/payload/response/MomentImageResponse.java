package space.nebula.nexus.payload.response;

import java.io.Serializable;

public record MomentImageResponse(Long id, Long fileId, String originalName, String fileUrl, String thumbnailUrl,
		Integer width, Integer height, String altText, Integer sortOrder) implements Serializable {
}
