package space.nebula.nexus.payload.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Authenticated user's profile and granted access claims")
public record UserInfoResponse(@Schema(description = "User ID") Long id,
		@Schema(description = "Immutable login username") String username,
		@Schema(description = "Public display name") String nickname, @Schema(description = "Avatar URL") String avatar,
		@Schema(description = "Granted role codes, for example ROLE_USER") Set<String> roles,
		@Schema(description = "Granted permission codes usable for frontend feature gating") Set<String> permissions) {
}
