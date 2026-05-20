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
 * Custom annotation for password validation. Checks for minimum length, special
 * characters, and digits.
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Password {
	String message() default "Password must be at least 8 characters long and contain at least one digit and one special character";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
}
