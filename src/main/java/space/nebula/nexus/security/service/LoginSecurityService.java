package space.nebula.nexus.security.service;

/**
 * Service for managing login security, including failure counting and lockouts.
 */
public interface LoginSecurityService
{

	/**
	 * Checks if a user's account is currently locked. Throws a BusinessException if
	 * locked.
	 *
	 * @param username the username to check
	 */
	void validateLoginLock(String username);

	/**
	 * Records a failed login attempt for the specified user. Increments the failure
	 * count and applies lock if threshold reached.
	 *
	 * @param username the username that failed to login
	 */
	void recordLoginFailure(String username);

	/**
	 * Resets the login failure count for the specified user. Usually called after a
	 * successful login.
	 *
	 * @param username the username to reset
	 */
	void resetLoginFailure(String username);
}
