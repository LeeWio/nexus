package space.nebula.nexus.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import space.nebula.nexus.common.validator.annotation.Password;

import java.util.regex.Pattern;

/**
 * Implementation of the password policy shared by account entry points.
 */
public class PasswordValidator implements ConstraintValidator<Password, String> {

	// At least 8 characters, one lowercase letter, one uppercase letter, one digit,
	// and one supported special character.
	private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-#])[A-Za-z\\d@$!%*?&._\\-#]{8,}$";
	private static final Pattern PATTERN = Pattern.compile(PASSWORD_PATTERN);

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return false;
		}
		return PATTERN.matcher(value).matches();
	}
}
