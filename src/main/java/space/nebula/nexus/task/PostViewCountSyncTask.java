package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.utils.RedisUtil;

import java.util.Map;

/**
 * Task to sync accumulated view counts from Redis hash to MySQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewCountSyncTask {

	private final RedisUtil redisUtil;
	private final PostRepository postRepository;

	private static final String LOCK_KEY = "nexus:lock:view-count-sync";

	/**
	 * Sync view counts every 10 minutes.
	 */
	@Scheduled(fixedRate = 600000)
	@Transactional
	public void syncViewCounts() {
		if (!redisUtil.lock(LOCK_KEY, "locked", 590, java.util.concurrent.TimeUnit.SECONDS)) {
			return;
		}

		Map<Object, Object> viewCounts = redisUtil.hashGetAllAndDelete(CacheConstants.POST_VIEW_EXTRA_HASH);
		if (viewCounts == null || viewCounts.isEmpty())
			return;

		log.info("Syncing view counts for {} posts from Redis to MySQL...", viewCounts.size());

		viewCounts.forEach((postIdObj, countObj) -> {
			try {
				Long postId = Long.valueOf(postIdObj.toString());
				Long count = Long.valueOf(countObj.toString());

				if (count > 0) {
					postRepository.incrementViews(postId, count);
				}
			} catch (Exception e) {
				log.error("Failed to sync view count for post id: {}", postIdObj, e);
			}
		});

		log.info("View count sync completed.");
	}
}
