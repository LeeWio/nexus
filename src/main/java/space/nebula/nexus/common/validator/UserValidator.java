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

	// Regex for professional password: at least 8 chars, 1 uppercase, 1 lowercase, 1 number, 1 special char (supporting periods, underscores, hyphens, and hashes)
	private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-#])[A-Za-z\\d@$!%*?&._\\-#]{8,}$";

	/**
	 * Validates if the registration request meets all business constraints.
	 */
	public void validateRegistration(RegisterRequest request) {
		// 1. Basic format checks using Hutool
		Assert.isFalse(StrUtil.isBlank(request.username()) || request.username().length() < 4,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "用户名长度不能少于 4 个字符"));

		Assert.isTrue(Validator.isEmail(request.email()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "邮箱格式不正确，请输入有效的邮箱地址"));

		// 2. Password strength validation
		Assert.isTrue(ReUtil.isMatch(PASSWORD_PATTERN, request.password()),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"密码必须至少为 8 位字符，且必须包含大小写字母、数字和特殊字符"));

		// 3. Uniqueness checks
		Assert.isFalse(userRepository.existsByUsername(request.username()),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
						"用户名 '" + request.username() + "' 已被占用，请更换用户名"));

		Assert.isFalse(userRepository.existsByEmail(request.email()),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
						"邮箱 '" + request.email() + "' 已被注册，您可以直接登录或更换邮箱"));
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
				() -> new BusinessException(BusinessCode.USER_NOT_FOUND, "未找到该用户：" + username));
	}
}
