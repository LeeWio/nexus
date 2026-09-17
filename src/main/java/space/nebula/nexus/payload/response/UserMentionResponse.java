package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;

/**
 * Public-safe user summary for @mention pickers. Intentionally omits email,
 * roles, and account status.
 */
@Schema(description = "Mentionable user summary")
public record UserMentionResponse(@Schema(description = "User id") Long id,
		@Schema(description = "Unique username") String username,
		@Schema(description = "Display nickname") String nickname,
		@Schema(description = "Avatar URL") String avatar) implements Serializable {
	private static final long serialVersionUID = 1L;
}
