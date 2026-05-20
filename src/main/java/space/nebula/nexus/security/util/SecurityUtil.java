package space.nebula.nexus.security.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.UserRepository;

/**
 * Utility class for Spring Security related operations.
 */
@UtilityClass
public class SecurityUtil {

	/**
	 * Get the username of the currently authenticated user.
	 */
	public String getCurrentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| "anonymousUser".equals(authentication.getPrincipal())) {
			return null;
		}
		return authentication.getName();
	}

	/**
	 * Get the currently authenticated user entity.
	 * 
	 * @throws BusinessException
	 *             if user is not authenticated or not found in database.
	 */
	public User getCurrentUserOrThrow(UserRepository userRepository) {
		String username = getCurrentUsername();
		if (username == null) {
			throw new BusinessException(401, "User not authenticated");
		}
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(404, "Current user not found"));
	}

	/**
	 * Check if current user has a specific role.
	 */
	public boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null)
			return false;
		return authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals(role) || a.getAuthority().equals("ROLE_" + role));
	}
}
