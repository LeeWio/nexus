package space.nebula.nexus.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for logging business operations.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogOperation {
    /**
     * Operation description (e.g., "Create Blog Post").
     */
    String value() default "";

    /**
     * Whether to log the method arguments.
     */
    boolean logArgs() default true;

    /**
     * Whether to log the method result.
     */
    boolean logResult() default false;
}
