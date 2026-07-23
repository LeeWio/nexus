package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
	@SchedulerLock(name = "weeklyNewsletter", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
	public void runWeeklyNewsletter() {
		log.info("Executing scheduled weekly newsletter broadcasting...");
		newsletterService.sendWeeklyNewsletter();
	}
}
