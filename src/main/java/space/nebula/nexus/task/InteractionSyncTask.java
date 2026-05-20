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

@Slf4j
@Component
@RequiredArgsConstructor
public class InteractionSyncTask {

	private final RedisTemplate<String, Object> redisTemplate;
	private final PostRepository postRepository;
	private final RedisUtil redisUtil;

	private static final String LOCK_KEY = "nexus:lock:interaction-sync";

	/**
	 * Synchronizes social interaction counts (likes, favorites) from Redis sets to
	 * the database. This periodically updates the denormalized count fields in the
	 * Post table for performance.
	 */
	@Scheduled(fixedRate = 120000) // Runs every 2 minutes
	@Transactional
	public void synchronizeSocialInteractions() {
		if (!redisUtil.lock(LOCK_KEY, "locked", 110, TimeUnit.SECONDS)) {
			return;
		}

		log.info("Starting social interaction synchronization (Likes/Favorites) from Redis to MySQL...");

		java.util.Set<Long> activePostIds = new java.util.HashSet<>();

		// Helper to scan a specific prefix and collect IDs
		java.util.function.Consumer<String> scanPrefix = (prefix) -> {
			ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(100).build();
			try (Cursor<String> cursor = redisTemplate.scan(options)) {
				while (cursor.hasNext()) {
					String key = cursor.next();
					try {
						Long activePostId = Long.valueOf(key.substring(prefix.length()));
						activePostIds.add(activePostId);
					} catch (Exception itemException) {
						log.error("Error extracting ID from key: {}", key, itemException);
					}
				}
			} catch (Exception e) {
				log.error("Error scanning prefix: {}", prefix, e);
			}
		};

		// Scan both likes and favorites sets
		scanPrefix.accept(CacheConstants.POST_LIKES_SET);
		scanPrefix.accept(CacheConstants.POST_FAVORITES_SET);

		int processedInteractionsCount = 0;
		for (Long activePostId : activePostIds) {
			try {
				String likeSetKey = CacheConstants.POST_LIKES_SET + activePostId;
				String favoriteSetKey = CacheConstants.POST_FAVORITES_SET + activePostId;

				// Get current sizes from Redis
				Long currentLikesCount = redisTemplate.opsForSet().size(likeSetKey);
				Long currentFavoritesCount = redisTemplate.opsForSet().size(favoriteSetKey);

				// Update DB if data found
				postRepository.findById(activePostId).ifPresent(postEntity -> {
					boolean hasStateChanged = false;

					if (currentLikesCount != null && !currentLikesCount.equals(postEntity.getLikesCount())) {
						postEntity.setLikesCount(currentLikesCount);
						hasStateChanged = true;
					}
					if (currentFavoritesCount != null
							&& !currentFavoritesCount.equals(postEntity.getFavoritesCount())) {
						postEntity.setFavoritesCount(currentFavoritesCount);
						hasStateChanged = true;
					}

					if (hasStateChanged) {
						postRepository.save(postEntity);
						log.debug("Synchronized interactions for Post ID: {}. Likes: {}, Favs: {}", activePostId,
								currentLikesCount, currentFavoritesCount);
					}
				});
				processedInteractionsCount++;
			} catch (Exception itemException) {
				log.error("Error synchronizing interactions for post: {}", activePostId, itemException);
			}
		}
		log.info("Finished social interaction synchronization. Processed {} active posts.", processedInteractionsCount);
	}
}
