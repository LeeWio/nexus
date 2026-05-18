package space.nebula.nexus.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import space.nebula.nexus.common.validator.annotation.Password;

import java.util.regex.Pattern;

/**
 * Implementation of custom password validator.
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {

    // Regex: At least 8 chars, one digit, one special char
    private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$";
    private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return PATTERN.matcher(value).matches();
    }
}
