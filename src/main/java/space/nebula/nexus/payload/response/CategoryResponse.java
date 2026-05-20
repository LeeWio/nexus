package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Category response payload.
 */
@Builder
@Schema(description = "Category details")
public record CategoryResponse(@Schema(description = "Category ID") Long id,

		@Schema(description = "Category name") String name,

		@Schema(description = "Category slug") String slug,

		@Schema(description = "Category description") String description,

		@Schema(description = "Creation time") LocalDateTime createdAt) implements Serializable {
	private static final long serialVersionUID = 1L;
}
