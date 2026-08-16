package space.nebula.nexus.payload.response;

import java.io.Serializable;

public record FileResponse(Long id, String fileName, String originalName, String fileUrl, String thumbnailUrl,
		Integer width, Integer height, Long fileSize, String fileType,
		java.time.LocalDateTime createdAt) implements Serializable {
	private static final long serialVersionUID = 1L;
}
