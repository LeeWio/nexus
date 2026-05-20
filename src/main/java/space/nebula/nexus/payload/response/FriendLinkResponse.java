package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import space.nebula.nexus.enums.FriendLinkStatus;

import java.io.Serializable;
import java.time.LocalDateTime;

@Schema(description = "Friend Link details")
public record FriendLinkResponse(@Schema(description = "Link ID") Long id,

		@Schema(description = "Site name") String name,

		@Schema(description = "Site URL") String url,

		@Schema(description = "Avatar URL") String avatar,

		@Schema(description = "Description") String description,

		@Schema(description = "Contact email") String email,

		@Schema(description = "Moderation status") FriendLinkStatus status,

		@Schema(description = "Display order") Integer sortOrder,

		@Schema(description = "Whether the link is published") Boolean isPublished,

		@Schema(description = "Creation time") LocalDateTime createdAt,

		@Schema(description = "Last update time") LocalDateTime updatedAt) implements Serializable {
}
