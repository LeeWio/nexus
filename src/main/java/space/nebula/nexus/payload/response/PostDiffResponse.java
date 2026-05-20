package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Comparison result between two post versions")
public record PostDiffResponse(@Schema(description = "Comparison for the title field") FieldDiff title,

		@Schema(description = "Comparison for the summary field") FieldDiff summary,

		@Schema(description = "Comparison for the content field") FieldDiff content) {
	public record FieldDiff(@Schema(description = "Value in the original/older version") String original,

			@Schema(description = "Value in the new/revised version") String revised,

			@Schema(description = "Whether the field has changed") boolean changed) {
	}
}
