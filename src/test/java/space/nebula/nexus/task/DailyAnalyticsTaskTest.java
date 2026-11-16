package space.nebula.nexus.task;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;
import space.nebula.nexus.config.AnalyticsProperties;
import space.nebula.nexus.service.AnalyticsTime;
import space.nebula.nexus.service.IAnalyticsService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DailyAnalyticsTaskTest {

	@Test
	void scheduleAndDatesUseShanghaiEvenWhileUtcIsStillOnThePreviousDate() throws Exception {
		var service = mock(IAnalyticsService.class);
		var time = new AnalyticsTime(
				Clock.fixed(Instant.parse("2026-11-15T16:05:00Z"), ZoneId.of("America/Los_Angeles")));
		new DailyAnalyticsTask(service, new AnalyticsProperties(), time).runDailyAggregation();
		verify(service).aggregateDailyData(LocalDate.of(2026, 11, 13));
		verify(service).aggregateDailyData(LocalDate.of(2026, 11, 14));
		verify(service).aggregateDailyData(LocalDate.of(2026, 11, 15));
		verify(service).purgeOldLogs(90);
		assertEquals("Asia/Shanghai",
				DailyAnalyticsTask.class.getMethod("runDailyAggregation").getAnnotation(Scheduled.class).zone());
	}
}
