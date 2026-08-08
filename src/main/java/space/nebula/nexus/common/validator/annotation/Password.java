package space.nebula.nexus.common.validator.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import space.nebula.nexus.common.validator.PasswordValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for password validation. Requires a minimum length plus
 * upper-case, lower-case, numeric, and special characters.
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
	String message() default "Password must be at least 8 characters long and contain upper-case letters, lower-case letters, a digit, and a special character";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}
