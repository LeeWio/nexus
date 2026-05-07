package space.nebula.nexus.payload.response;

import lombok.Builder;
import java.io.Serializable;
import java.util.Set;

/**
 * Authentication response using Java 21 Record.
 */
@Builder
public record AuthResponse(
    String accessToken,
    String tokenType,
    String username,
    Set<String> roles
) implements Serializable {
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
