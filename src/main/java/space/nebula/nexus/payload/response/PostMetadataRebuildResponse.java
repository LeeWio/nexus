package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of an article metadata rebuild")
public record PostMetadataRebuildResponse(
		@Schema(description = "Number of posts scanned") long processed,
		@Schema(description = "Number of posts whose metadata changed") long updated,
		@Schema(description = "Whether the operation only calculated changes") boolean dryRun) {
}
