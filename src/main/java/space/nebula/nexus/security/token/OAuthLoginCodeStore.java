package space.nebula.nexus.security.token;

import space.nebula.nexus.payload.response.AuthResponse;

import java.util.Optional;

/**
 * One-time store for OAuth login handoff codes. Codes map to a freshly issued
 * auth response and must be consumed exactly once within a short TTL.
 */
public interface OAuthLoginCodeStore {

	/**
	 * Persist an auth response under a newly generated opaque code.
	 *
	 * @return the opaque code to place in the frontend redirect URL
	 */
	String issue(AuthResponse authResponse);

	/**
	 * Atomically read and delete the auth response for a code.
	 */
	Optional<AuthResponse> consume(String code);
}
