package space.nebula.nexus.service;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Event timestamps are UTC; reporting dates are calendar dates in Shanghai. */
@Component
public class AnalyticsTime {

	public static final String REPORTING_ZONE = "Asia/Shanghai";
	private static final ZoneId ZONE = ZoneId.of(REPORTING_ZONE);
	private final Clock clock;

	public AnalyticsTime() {
		this(Clock.systemUTC());
	}

	public AnalyticsTime(Clock clock) {
		this.clock = clock;
	}

	public Instant now() {
		return clock.instant();
	}

	public LocalDate today() {
		return now().atZone(ZONE).toLocalDate();
	}

	public Instant startOfDay(LocalDate date) {
		return date.atStartOfDay(ZONE).toInstant();
	}

	public Window day(LocalDate date) {
		return new Window(startOfDay(date), startOfDay(date.plusDays(1)));
	}

	/** Includes today and the preceding days, not an extra calendar day. */
	public Window recentDays(int days) {
		Instant end = now();
		LocalDate firstDate = end.atZone(ZONE).toLocalDate().minusDays(Math.clamp(days, 1, 365) - 1L);
		return new Window(startOfDay(firstDate), end);
	}

	public int reportingOffsetSeconds(Instant instant) {
		return ZONE.getRules().getOffset(instant).getTotalSeconds();
	}

	/** All reporting windows are half-open: [start, end). */
	public record Window(Instant start, Instant end) {
		public Window previous() {
			return new Window(start.minus(Duration.between(start, end)), start);
		}
	}
}
