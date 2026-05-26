package space.nebula.nexus.common.validator;

import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
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

	// Regex for professional password: at least 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char
	private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

	/**
	 * Validates if the registration request meets all business constraints.
	 */
	public void validateRegistration(RegisterRequest request) {
		// 1. Basic format checks using Hutool
		if (StrUtil.isBlank(request.username()) || request.username().length() < 4) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "Username must be at least 4 characters");
		}

		if (!Validator.isEmail(request.email())) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "Invalid email format");
		}

		// 2. Password strength validation
		if (!ReUtil.isMatch(PASSWORD_PATTERN, request.password())) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, 
					"Password must be at least 8 characters long and include uppercase, lowercase, numbers, and special characters");
		}

		// 3. Uniqueness checks
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
