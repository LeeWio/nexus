package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Tag response payload.
 */
@Builder
@Schema(description = "Tag details")
public record TagResponse(@Schema(description = "Tag ID") Long id,

		@Schema(description = "Tag name") String name,

		@Schema(description = "Tag slug") String slug,

		@Schema(description = "Creation time") LocalDateTime createdAt) implements Serializable {
	private static final long serialVersionUID = 1L;
}
