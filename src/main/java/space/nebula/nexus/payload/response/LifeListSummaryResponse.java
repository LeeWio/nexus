package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Counts for the life-list overview. Public counts exclude private and archived wishes.")
public record LifeListSummaryResponse(
		@Schema(description = "Completed wishes in this view") long completed,
		@Schema(description = "Non-archived wishes in this view") long total,
		boolean includesPrivate) {
}
