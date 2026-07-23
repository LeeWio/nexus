package space.nebula.nexus.security.token;

import java.time.Duration;

/** Stores access tokens that must no longer be accepted. */
public interface RevokedTokenStore {

	/**
	 * Revokes a token for its remaining lifetime.
	 *
	 * @param token
	 *            encoded access token
	 * @param timeToLive
	 *            remaining token lifetime
	 */
	void revoke(String token, Duration timeToLive);

	/**
	 * Checks whether a token has been revoked.
	 *
	 * @param token
	 *            encoded access token
	 * @return {@code true} if the token is revoked
	 */
	boolean isRevoked(String token);
}
