package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A normalized social topic attached to a Moment")
public record MomentTopicResponse(
		@Schema(example = "42") Long id,
		@Schema(example = "frontend-architecture") String slug) {
}
