package space.nebula.nexus.common.validator.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import space.nebula.nexus.common.validator.SlugValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for URL slug validation.
 * Ensures the slug only contains lowercase letters, numbers, and hyphens.
 */
@Documented
@Constraint(validatedBy = SlugValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Slug {
    String message() default "Slug must only contain lowercase alphanumeric characters and hyphens (e.g. 'my-cool-post')";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
