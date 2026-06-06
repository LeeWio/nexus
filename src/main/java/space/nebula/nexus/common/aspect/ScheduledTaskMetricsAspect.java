package space.nebula.nexus.common.aspect;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect to capture metrics for scheduled tasks.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledTaskMetricsAspect {

	private final MeterRegistry meterRegistry;

	/**
	 * Measure the execution time of methods annotated with @Scheduled.
	 */
	@Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
	public Object profile(ProceedingJoinPoint pjp) throws Throwable {
		String methodName = pjp.getSignature().toShortString();

		Timer.Sample sample = Timer.start(meterRegistry);
		try {
			return pjp.proceed();
		} finally {
			sample.stop(Timer.builder("nexus.scheduled.task").description("Duration of scheduled tasks")
					.tag("method", methodName).register(meterRegistry));
		}
	}
}
