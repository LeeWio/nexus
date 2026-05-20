package space.nebula.nexus.payload.response;

import java.util.Set;

public record UserInfoResponse(Long id, String username, String nickname, String avatar, Set<String> roles,
		Set<String> permissions) {
}
