package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.IGitHubService;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubSyncTask
{

	private final IGitHubService githubService;

	/**
	 * Periodically sync project metrics from GitHub. Runs every 12 hours.
	 */
	@Scheduled(cron = "0 0 0/12 * * ?")
	@SchedulerLock(name = "githubMetricsSync", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
	public void syncGitHubMetrics()
	{
		log.info("Commencing scheduled GitHub metrics synchronization...");
		try
		{
			githubService.synchronizeProjectMetrics();
		}
		catch (Exception e)
		{
			log.error("Scheduled GitHub synchronization failed", e);
		}
	}
}
