package space.nebula.nexus.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import space.nebula.nexus.common.validator.annotation.Slug;

import java.util.regex.Pattern;

/**
 * Implementation of custom slug validator.
 */
public class SlugValidator implements ConstraintValidator<Slug, String> {

	private static final String SLUG_PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";
	private static final Pattern PATTERN = Pattern.compile(SLUG_PATTERN);

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true; // Use @NotBlank if it's required
		}
		return PATTERN.matcher(value).matches();
	}
}
