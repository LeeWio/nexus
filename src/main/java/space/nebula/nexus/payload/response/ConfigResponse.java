package space.nebula.nexus.payload.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConfigResponse {
	private Long id;
	private String configKey;
	private String configValue;
	private String configName;
	private String description;
	private Boolean isPublic;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
