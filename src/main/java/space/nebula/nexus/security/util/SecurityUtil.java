package space.nebula.nexus.security.util;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.model.SecurityUser;

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
		if (ObjectUtil.isNull(authentication) || !authentication.isAuthenticated()
				|| ObjectUtil.equal("anonymousUser", authentication.getPrincipal())) {
			return null;
		}
		return authentication.getName();
	}

	/**
	 * Get the currently authenticated User entity directly from the security context principal,
	 * completely bypassing database lookups when authenticated via standard JWT/form.
	 */
	public User getCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (ObjectUtil.isNull(authentication) || !authentication.isAuthenticated()) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof SecurityUser securityUser) {
			return securityUser.getUser();
		}
		return null;
	}

	/**
	 * Get the currently authenticated user entity.
	 * 
	 * @throws BusinessException
	 *             if user is not authenticated or not found in database.
	 */
	public User getCurrentUserOrThrow(UserRepository userRepository) {
		User currentUser = getCurrentUser();
		if (currentUser != null) {
			return currentUser;
		}
		String username = getCurrentUsername();
		Assert.notNull(username, () -> new BusinessException(401, "Authentication required"));
		return userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(404, "Current user could not be resolved"));
	}

	/**
	 * Check if current user has a specific role.
	 */
	public boolean hasRole(String role) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (ObjectUtil.isNull(authentication))
			return false;
		return authentication.getAuthorities().stream()
				.anyMatch(a -> a.getAuthority().equals(role) || a.getAuthority().equals("ROLE_" + role));
	}
}
