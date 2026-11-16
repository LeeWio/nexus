package space.nebula.nexus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyticsTimeTest {

	@ParameterizedTest
	@ValueSource(strings = {"UTC", "Asia/Shanghai", "America/Los_Angeles"})
	void reportingDateIsIndependentOfClockZone(String zone) {
		var time = new AnalyticsTime(Clock.fixed(Instant.parse("2026-11-15T16:00:00Z"), ZoneId.of(zone)));
		assertEquals(LocalDate.of(2026, 11, 16), time.today());
		assertEquals(Instant.parse("2026-11-15T16:00:00Z"), time.day(time.today()).start());
		assertEquals(Instant.parse("2026-11-16T16:00:00Z"), time.day(time.today()).end());
	}

	@Test
	void sevenDaysContainsTodayAndSixPriorCalendarDates() {
		var time = new AnalyticsTime(Clock.fixed(Instant.parse("2026-11-16T06:37:00Z"), ZoneId.of("UTC")));
		var window = time.recentDays(7);
		assertEquals(Instant.parse("2026-11-09T16:00:00Z"), window.start());
		assertEquals(Instant.parse("2026-11-16T06:37:00Z"), window.end());
		assertEquals(Duration.between(window.start(), window.end()),
				Duration.between(window.previous().start(), window.previous().end()));
		assertEquals(window.start(), window.previous().end());
	}

	@Test
	void windowsClampToOneThrough365Days() {
		var time = new AnalyticsTime(Clock.fixed(Instant.parse("2026-11-16T06:37:00Z"), ZoneId.of("UTC")));
		assertEquals(time.recentDays(1), time.recentDays(0));
		assertEquals(time.recentDays(365), time.recentDays(999));
	}
}
