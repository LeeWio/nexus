package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.utils.RedisUtil;

import java.util.concurrent.TimeUnit;

/**
 * Scheduled tasks for Post-related background processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostTask
{

	private final PostRepository postRepository;

	private final RedisUtil redisUtil;

	private static final String LOCK_KEY = "nexus:lock:publish-posts";

	/**
	 * Publish posts that are scheduled and their time has reached. Runs every
	 * minute.
	 */
	@Scheduled(cron = "0 * * * * ?")
	@Transactional
	public void publishScheduledPosts()
	{
		if (!redisUtil.lock(LOCK_KEY, "locked", 50, TimeUnit.SECONDS))
		{
			return;
		}

		java.time.LocalDateTime now = java.time.LocalDateTime.now();
		int updated = postRepository.updateScheduledPosts(now);
		if (updated > 0)
		{
			log.info("Published {} scheduled posts at {}", updated, now);
		}
	}
}
