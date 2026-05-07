package space.nebula.nexus.payload.response;

import space.nebula.nexus.enums.UserStatus;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

public record UserResponse(
    Long id,
    String username,
    String email,
    String nickname,
    UserStatus status,
    LocalDateTime createdAt,
    Set<String> roles
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
