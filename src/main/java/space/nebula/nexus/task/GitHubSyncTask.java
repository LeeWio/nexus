package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import space.nebula.nexus.service.IGitHubService;
import space.nebula.nexus.utils.RedisUtil;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubSyncTask
{

	private final IGitHubService githubService;
	private final RedisUtil redisUtil;

	private static final String LOCK_KEY = "nexus:lock:github-sync";

	/**
	 * Periodically sync project metrics from GitHub. Runs every 12 hours.
	 */
	@Scheduled(cron = "0 0 0/12 * * ?")
	public void syncGitHubMetrics()
	{
		if (!redisUtil.lock(LOCK_KEY, "locked", 11, TimeUnit.HOURS))
		{
			log.debug("GitHub metrics sync task already running on another instance.");
			return;
		}

		log.info("Commencing scheduled GitHub metrics synchronization...");
		try
		{
			githubService.synchronizeProjectMetrics();
		}
		catch (Exception e)
		{
			log.error("Scheduled GitHub synchronization failed", e);
			// If it fails, we might want to release the lock early,
			// but for a task that runs every 12h, leaving it locked for 11h is safer
			// to avoid immediate retries by other nodes.
		}
	}
}
