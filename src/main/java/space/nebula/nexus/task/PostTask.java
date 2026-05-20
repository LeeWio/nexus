package space.nebula.nexus.task;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.repository.PostRepository;

/**
 * Scheduled tasks for Post-related background processing.
 */
@Slf4j
@Component
public class PostTask {

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
}
