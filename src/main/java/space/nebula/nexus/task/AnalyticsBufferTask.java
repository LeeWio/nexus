package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.VisitLog;
import space.nebula.nexus.repository.VisitLogRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsBufferTask {

    private final RedisTemplate<String, Object> redisTemplate;
    private final VisitLogRepository visitLogRepository;

    private static final String ANALYTICS_BUFFER_KEY = "nexus:analytics:buffer";
    private static final int BATCH_SIZE = 100;

    /**
     * Periodically flushes buffered analytics logs from Redis to MySQL.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void flushAnalyticsBuffer() {
        log.info("Commencing batch persistence of buffered analytics logs...");
        
        Long bufferSize = redisTemplate.opsForList().size(ANALYTICS_BUFFER_KEY);
        if (bufferSize == null || bufferSize == 0) {
            log.info("Analytics buffer is empty. Skipping flush.");
            return;
        }

        List<VisitLog> logsToPersist = new ArrayList<>();
        int count = 0;

        while (count < bufferSize) {
            Object logEntry = redisTemplate.opsForList().leftPop(ANALYTICS_BUFFER_KEY);
            if (logEntry instanceof VisitLog visitLog) {
                // Future Enhancement: Add UA parsing here
                logsToPersist.add(visitLog);
                count++;
            } else {
                break;
            }

            // Batch insert every BATCH_SIZE to manage memory and transaction size
            if (logsToPersist.size() >= BATCH_SIZE) {
                visitLogRepository.saveAll(logsToPersist);
                logsToPersist.clear();
            }
        }

        if (!logsToPersist.isEmpty()) {
            visitLogRepository.saveAll(logsToPersist);
        }

        log.info("Successfully persisted {} analytics logs to database.", count);
    }
}
