package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Structured navigation intent. Object identifiers do not grant permission to the linked resource.")
public record NotificationContext(ObjectType objectType, Long objectId, Long actorId, Action action) {
	public enum ObjectType {
		POST, COMMENT, FRIEND_LINK, USER
	}
	public enum Action {
		VIEW, REVIEW, EDIT, REVIEW_REPORT
	}

	public NotificationContext {
		if (objectType == null || objectId == null || objectId < 1 || action == null
				|| (actorId != null && actorId < 1))
			throw new IllegalArgumentException("Notification context requires a valid object and action");
	}
}
