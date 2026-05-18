package space.nebula.nexus.task;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.repository.PostRepository;

/**
 * Scheduled tasks for Post-related background processing.
 */
@Slf4j
@Component
public class PostTask {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private PostRepository postRepository;

    /**
     * Publish posts that are scheduled and their time has reached.
     * Runs every minute.
     */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void publishScheduledPosts() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int updated = postRepository.updateScheduledPosts(now);
        if (updated > 0) {
            log.info("Published {} scheduled posts at {}", updated, now);
        }
    }

    /**
     * Synchronizes accumulated post view counts from Redis back to the persistent MySQL database.
     * This ensures that real-time traffic data is eventually consistent with the primary store.
     * Runs every 30 minutes to balance performance and data safety.
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    @Transactional
    public void synchronizePostViewCounts() {
        log.info("Commencing background synchronization of post view counts from Redis cache...");
        
        ScanOptions viewKeyScanOptions = ScanOptions.scanOptions()
                .match(CacheConstants.POST_VIEW_COUNT + "*")
                .count(100)
                .build();

        try (Cursor<String> viewKeyCursor = redisTemplate.scan(viewKeyScanOptions)) {
            int synchronizedPostsCount = 0;
            while (viewKeyCursor.hasNext()) {
                String redisKey = viewKeyCursor.next();
                try {
                    // Extract ID from key: post:view_count:{id}
                    Long targetPostId = Long.valueOf(redisKey.substring(CacheConstants.POST_VIEW_COUNT.length()));
                    
                    Object cachedViewsData = redisTemplate.opsForValue().get(redisKey);
                    if (cachedViewsData instanceof Number deltaViews) {
                        postRepository.incrementViews(targetPostId, deltaViews.longValue());
                        
                        // Atomically remove the key only after successful DB update
                        redisTemplate.delete(redisKey);
                        synchronizedPostsCount++;
                        log.debug("Synchronized {} views for post ID: {}", deltaViews, targetPostId);
                    }
                } catch (Exception entryException) {
                    log.error("Failed to synchronize view count for key: {}. Skipping.", redisKey, entryException);
                }
            }
            log.info("Completed post view synchronization. Total unique posts updated: {}", synchronizedPostsCount);
        } catch (Exception scanException) {
            log.error("Critical error during post view synchronization scan process", scanException);
        }
    }
}
