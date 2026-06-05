package space.nebula.nexus.common.validator;

import cn.hutool.core.lang.Assert;
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
		Assert.isFalse(StrUtil.isBlank(request.username()) || request.username().length() < 4,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Username must be at least 4 characters"));

		Assert.isTrue(Validator.isEmail(request.email()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Invalid email format"));

		// 2. Password strength validation
		Assert.isTrue(ReUtil.isMatch(PASSWORD_PATTERN, request.password()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"Password must be at least 8 characters long and include uppercase, lowercase, numbers, and special characters"));

		// 3. Uniqueness checks
		Assert.isFalse(userRepository.existsByUsername(request.username()),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
						"Username '" + request.username() + "' is already taken"));

		Assert.isFalse(userRepository.existsByEmail(request.email()),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
						"Email '" + request.email() + "' is already in use"));
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
		Assert.isTrue(userRepository.existsByUsername(username),
				() -> new BusinessException(BusinessCode.USER_NOT_FOUND, "User not found: " + username));
	}
}
