package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.utils.RedisUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Task to sync accumulated view counts from Redis hash to MySQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewCountSyncTask
{

	private final RedisUtil redisUtil;
	private final JdbcTemplate jdbcTemplate;

	private static final String LOCK_KEY = "nexus:lock:view-count-sync";
	private static final String UPDATE_SQL = "UPDATE blog_post SET views = views + ? WHERE id = ?";

	/**
	 * Sync view counts every 10 minutes using batch updates for performance.
	 */
	@Scheduled(fixedRate = 600000)
	@Transactional
	public void syncViewCounts()
	{
		if (!redisUtil.lock(LOCK_KEY, "locked", 590, java.util.concurrent.TimeUnit.SECONDS))
		{
			return;
		}

		Map<Object, Object> viewCounts = redisUtil.hashGetAllAndDelete(CacheConstants.POST_VIEW_EXTRA_HASH);
		if (viewCounts == null || viewCounts.isEmpty())
		{
			return;
		}

		log.info("Syncing view counts for {} posts from Redis to MySQL in batch...", viewCounts.size());

		List<Object[]> batchArgs = new ArrayList<>();
		viewCounts.forEach((postIdObj, countObj) ->
		{
			try
			{
				Long postId = Long.valueOf(postIdObj.toString());
				Long count = Long.valueOf(countObj.toString());
				if (count > 0)
				{
					batchArgs.add(new Object[] { count, postId });
				}
			}
			catch (Exception e)
			{
				log.error("Failed to parse view count for post id: {}", postIdObj, e);
			}
		});

		if (!batchArgs.isEmpty())
		{
			try
			{
				jdbcTemplate.batchUpdate(UPDATE_SQL, batchArgs);
				log.info("Successfully synced {} post view records in batch.", batchArgs.size());
			}
			catch (Exception e)
			{
				log.error("Failed to execute batch update for view counts", e);
			}
		}
	}
}
