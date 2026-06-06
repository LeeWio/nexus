package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
    public void runWeeklyLinkCheck() {
        log.info("Executing scheduled weekly external link health check...");
        linkHealthService.runFullScan();
    }
}
