package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.IPostService;

/**
 * Scheduled tasks for Post-related background processing.
 */
@Component
@RequiredArgsConstructor
public class PostTask {

	private static final int PUBLICATION_BATCH_SIZE = 100;

	private final IPostService postService;

	/**
	 * Publish posts that are scheduled and their time has reached. Runs every
	 * minute.
	 */
	@Scheduled(cron = "0 * * * * ?")
	@SchedulerLock(name = "scheduledPostPublish", lockAtMostFor = "PT50S")
	public void publishScheduledPosts() {
		java.time.LocalDateTime now = java.time.LocalDateTime.now();
		postService.publishDueScheduledPosts(now, PUBLICATION_BATCH_SIZE);
	}
}
