package space.nebula.nexus.payload.response;

import java.io.Serializable;

public record FileResponse(
    String fileName,
    String fileUrl,
    Long fileSize,
    String fileType
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
