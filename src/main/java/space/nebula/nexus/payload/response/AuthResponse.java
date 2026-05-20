package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.io.Serializable;
import java.util.Set;

/**
 * Authentication response using Java 21 Record.
 */
@Builder
@Schema(description = "User authentication result with access token")
public record AuthResponse(@Schema(description = "JWT Access Token") String accessToken,

		@Schema(description = "Type of token", example = "Bearer") String tokenType,

		@Schema(description = "Authenticated username", example = "admin") String username,

		@Schema(description = "User's email address", example = "admin@example.com") String email,

		@Schema(description = "Set of roles assigned to the user", example = "[\"ROLE_ADMIN\"]") Set<String> roles)
		implements
			Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Compact constructor to handle defaults.
	 */
	public AuthResponse {
		if (tokenType == null) {
			tokenType = "Bearer";
		}
	}
}
