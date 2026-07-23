package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.OperationLog;
import space.nebula.nexus.repository.OperationLogRepository;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;

/**
 * Task to flush buffered operation logs from Redis to MySQL.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogBufferTask {

	private final RedisUtil redisUtil;
	private final OperationLogRepository operationLogRepository;

	/**
	 * Flush operation logs every 1 minute.
	 */
	@Scheduled(fixedRate = 60000)
	@SchedulerLock(name = "operationLogFlush", lockAtMostFor = "PT50S")
	@Transactional
	public void flushOperationLogs() {
		Long size = redisUtil.listSize(CacheConstants.OPERATION_LOG_BUFFER_KEY);
		if (size == null || size == 0)
			return;

		log.info("Flushing {} operation logs from Redis buffer...", size);

		// Pop in batches to reduce network roundtrips
		List<OperationLog> logsToPersist = redisUtil.listPopLeft(CacheConstants.OPERATION_LOG_BUFFER_KEY, size,
				OperationLog.class);

		if (!logsToPersist.isEmpty()) {
			operationLogRepository.saveAll(logsToPersist);
			log.info("Successfully persisted {} operation logs to database.", logsToPersist.size());
		}
	}
}
