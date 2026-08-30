package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Actionable editorial workflow items")
public record ContentWorkflowResponse(List<Item> items, Summary summary) {

	public record Item(String id, String type, String title, String description, String action, Priority priority,
			PostStatus status, LocalDateTime relevantAt, String href) {
	}

	public record Summary(long needsReview, long scheduled, long drafts, long rejected, long total) {
	}

	public enum Priority {
		HIGH, MEDIUM, LOW
	}
}
