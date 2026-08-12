package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Compact published article entry rendered on a public column page. */
@Schema(description = "Compact published article entry within a column")
public record ColumnPostResponse(Long id, String title, String slug, String coverImage, String summary,
		String authorName, Long views, Long likesCount, LocalDateTime publishedAt) {
}
