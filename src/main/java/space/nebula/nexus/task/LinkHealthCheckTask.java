package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.ILinkHealthService;

@Slf4j
@Component
@RequiredArgsConstructor
public class LinkHealthCheckTask {

    private final ILinkHealthService linkHealthService;

    /**
     * Runs once a week on Sunday at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @SchedulerLock(name = "linkHealthCheck", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void runWeeklyLinkCheck() {
        log.info("Executing scheduled weekly external link health check...");
        linkHealthService.runFullScan();
    }
}
