package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import space.nebula.nexus.config.AnalyticsProperties;
import space.nebula.nexus.service.IAnalyticsService;
import space.nebula.nexus.service.AnalyticsTime;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAnalyticsTask {

	private final IAnalyticsService analyticsService;
	private final AnalyticsProperties analyticsProperties;
	private final AnalyticsTime analyticsTime;

	/**
	 * Runs every day at 00:05 AM to aggregate data for the previous day.
	 */
	@Scheduled(cron = "0 5 0 * * ?", zone = AnalyticsTime.REPORTING_ZONE)
	@SchedulerLock(name = "dailyAnalytics", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
	public void runDailyAggregation() {
		LocalDate today = analyticsTime.today();
		for (int daysAgo = 3; daysAgo >= 1; daysAgo--) {
			LocalDate date = today.minusDays(daysAgo);
			log.info("Executing scheduled daily analytics aggregation for {}", date);
			analyticsService.aggregateDailyData(date);
		}

		analyticsService.purgeOldLogs(analyticsProperties.getRetentionDays());
	}
}
