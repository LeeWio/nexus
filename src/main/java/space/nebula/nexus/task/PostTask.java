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
     * Sync post view counts from Redis to Database every 30 minutes.
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    @Transactional
    public void syncPostViews() {
        log.info("Starting scheduled task: syncPostViews");
        
        ScanOptions options = ScanOptions.scanOptions()
                .match(CacheConstants.POST_VIEW_COUNT + "*")
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            int count = 0;
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long postId = Long.valueOf(key.substring(CacheConstants.POST_VIEW_COUNT.length()));
                
                Object viewsObj = redisTemplate.opsForValue().get(key);
                if (viewsObj instanceof Number views) {
                    postRepository.incrementViews(postId, views.longValue());
                    // Clear the key after syncing
                    redisTemplate.delete(key);
                    count++;
                }
            }
            log.info("Finished syncPostViews: synced {} posts", count);
        } catch (Exception e) {
            log.error("Error during syncPostViews task", e);
        }
    }
}
