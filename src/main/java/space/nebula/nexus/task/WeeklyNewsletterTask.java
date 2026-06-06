package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.INewsletterService;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyNewsletterTask {

    private final INewsletterService newsletterService;

    /**
     * Runs every Saturday at 10 AM.
     */
    @Scheduled(cron = "0 0 10 * * SAT")
    public void runWeeklyNewsletter() {
        log.info("Executing scheduled weekly newsletter broadcasting...");
        newsletterService.sendWeeklyNewsletter();
    }
}
