package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.utils.RedisUtil;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.utils.RedisUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionSyncTask
{

	private final RedisTemplate<String, Object> redisTemplate;
	private final JdbcTemplate jdbcTemplate;
	private final RedisUtil redisUtil;

	private static final String LOCK_KEY = "nexus:lock:interaction-sync";
	private static final String UPDATE_SQL = "UPDATE blog_post SET likes_count = ?, favorites_count = ? WHERE id = ?";

	/**
	 * Synchronizes social interaction counts (likes, favorites) from Redis sets to
	 * the database. This periodically updates the denormalized count fields in the
	 * Post table for performance using batch updates.
	 */
	@Scheduled(fixedRate = 120000) // Runs every 2 minutes
	@Transactional
	public void synchronizeSocialInteractions()
	{
		if (!redisUtil.lock(LOCK_KEY, "locked", 110, TimeUnit.SECONDS))
		{
			return;
		}

		log.info("Starting social interaction synchronization (Likes/Favorites) from Redis to MySQL...");

		java.util.Set<Long> activePostIds = new java.util.HashSet<>();

		// Helper to scan a specific prefix and collect IDs
		java.util.function.Consumer<String> scanPrefix = (prefix) ->
		{
			ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
			try (Cursor<String> cursor = redisTemplate.scan(options))
			{
				while (cursor.hasNext())
				{
					String key = cursor.next();
					try
					{
						Long activePostId = Long.valueOf(key.substring(prefix.length()));
						activePostIds.add(activePostId);
					}
					catch (Exception itemException)
					{
						log.error("Error extracting ID from key: {}", key, itemException);
					}
				}
			}
			catch (Exception e)
			{
				log.error("Error scanning prefix: {}", prefix, e);
			}
		};

		// Scan both likes and favorites sets
		scanPrefix.accept(CacheConstants.POST_LIKES_SET);
		scanPrefix.accept(CacheConstants.POST_FAVORITES_SET);

		if (activePostIds.isEmpty())
		{
			return;
		}

		List<Object[]> batchArgs = new ArrayList<>();
		for (Long activePostId : activePostIds)
		{
			try
			{
				String likeSetKey = CacheConstants.POST_LIKES_SET + activePostId;
				String favoriteSetKey = CacheConstants.POST_FAVORITES_SET + activePostId;

				Long currentLikesCount = redisTemplate.opsForSet().size(likeSetKey);
				Long currentFavoritesCount = redisTemplate.opsForSet().size(favoriteSetKey);

				batchArgs.add(new Object[] { 
						currentLikesCount != null ? currentLikesCount : 0L, 
						currentFavoritesCount != null ? currentFavoritesCount : 0L, 
						activePostId 
				});
			}
			catch (Exception e)
			{
				log.error("Error preparing interactions for post: {}", activePostId, e);
			}
		}

		if (!batchArgs.isEmpty())
		{
			try
			{
				jdbcTemplate.batchUpdate(UPDATE_SQL, batchArgs);
				log.info("Successfully synced interactions for {} posts in batch.", batchArgs.size());
			}
			catch (Exception e)
			{
				log.error("Failed to execute batch update for interactions", e);
			}
		}
	}
}
