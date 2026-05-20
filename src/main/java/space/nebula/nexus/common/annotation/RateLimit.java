package space.nebula.nexus.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * Annotation for frequency limit on methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

	/**
	 * Cache key prefix.
	 */
	String key() default "rate_limit:";

	/**
	 * Limit count.
	 */
	int count() default 10;

	/**
	 * Time window.
	 */
	long time() default 1;

	/**
	 * Time unit.
	 */
	TimeUnit unit() default TimeUnit.MINUTES;

	/**
	 * Error message.
	 */
	String message() default "Request frequency is too high, please try again later.";
}
