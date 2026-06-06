package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.IAnalyticsService;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAnalyticsTask {

    private final IAnalyticsService analyticsService;

    /**
     * Runs every day at 00:05 AM to aggregate data for the previous day.
     */
    @Scheduled(cron = "0 5 0 * * ?")
    public void runDailyAggregation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Executing scheduled daily analytics aggregation for {}", yesterday);
        analyticsService.aggregateDailyData(yesterday);
    }
}
