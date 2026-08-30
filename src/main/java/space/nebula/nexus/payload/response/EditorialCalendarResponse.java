package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.PostStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Editorial calendar entries for posts and moments")
public record EditorialCalendarResponse(LocalDate from, LocalDate to, List<Entry> entries) {

	public record Entry(String id, String type, String title, LocalDate date, LocalDateTime timestamp,
			PostStatus status, String href) {
	}
}
