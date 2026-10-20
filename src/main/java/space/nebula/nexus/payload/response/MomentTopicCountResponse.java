package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How many public moments use one topic.
 */
@Schema(description = "A public moment topic and how many published moments use it")
public record MomentTopicCountResponse(@Schema(description = "Normalized topic slug", example = "travel") String slug,
		@Schema(description = "Published public moments that include this topic", example = "4") long count) {
}
