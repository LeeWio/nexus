package space.nebula.nexus.common.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.repository.UserRepository;

/**
 * Validator for user-related business logic. Ensures data integrity and
 * uniqueness constraints during registration and authentication.
 */
@Component
@RequiredArgsConstructor
public class UserValidator {

	private final UserRepository userRepository;

	/**
	 * Validates if the registration request meets all business constraints.
	 * 
	 * @param request
	 *            the registration details
	 * @throws BusinessException
	 *             if username or email is already in use
	 */
	public void validateRegistration(RegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new BusinessException(BusinessCode.DUPLICATE_KEY,
					"Username '" + request.username() + "' is already taken");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(BusinessCode.DUPLICATE_KEY,
					"Email '" + request.email() + "' is already in use");
		}
	}

	/**
	 * Verifies if a specific username exists in the system.
	 * 
	 * @param username
	 *            the username to check
	 * @throws BusinessException
	 *             if the user is not found
	 */
	public void checkUsernameExists(String username) {
		if (!userRepository.existsByUsername(username)) {
			throw new BusinessException(BusinessCode.USER_NOT_FOUND, "User not found: " + username);
		}
	}
}
