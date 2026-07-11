package space.nebula.nexus.security.token;

import java.time.Duration;

/** Maintains one-time refresh-token sessions. */
public interface RefreshTokenStore {

	void issue(String tokenId, String username, Duration timeToLive);

	/** Atomically consumes a refresh token so it cannot be replayed. */
	boolean consume(String tokenId, String username);
}
