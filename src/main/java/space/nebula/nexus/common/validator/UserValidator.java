package space.nebula.nexus.common.validator;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.repository.UserRepository;

/**
 * Validator for user-related business logic.
 */
@Component
public class UserValidator {

    @Resource
    private UserRepository userRepository;

    /**
     * Validates if the registration request is valid.
     */
    public void validateRegistration(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(BusinessCode.DUPLICATE_KEY, "Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(BusinessCode.DUPLICATE_KEY, "Email '" + request.email() + "' is already in use");
        }
    }
    
    /**
     * Checks if a username exists.
     */
    public void checkUsernameExists(String username) {
        if (!userRepository.existsByUsername(username)) {
            throw new BusinessException(BusinessCode.USER_NOT_FOUND, "User not found: " + username);
        }
    }
}
