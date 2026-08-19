package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.entity.VisitLog;
import space.nebula.nexus.repository.VisitLogRepository;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsBufferTask {

	private final RedisUtil redisUtil;
	private final VisitLogRepository visitLogRepository;

	private static final int BATCH_SIZE = 100;

	/**
	 * Periodically flushes buffered analytics logs from Redis to MySQL. Runs every
	 * 5 minutes.
	 */
	@Scheduled(fixedRate = 300000)
	@SchedulerLock(name = "analyticsBufferFlush", lockAtMostFor = "PT4M")
	@Transactional
	public void flushAnalyticsBuffer() {
		log.info("Commencing batch persistence of buffered analytics logs...");

		Long bufferSize = redisUtil.listSize(CacheConstants.ANALYTICS_BUFFER_KEY);
		if (bufferSize == null || bufferSize == 0) {
			log.info("Analytics buffer is empty. Skipping flush.");
			return;
		}

		// Pop in batches to reduce network roundtrips
		List<VisitLog> bufferedLogs = redisUtil.listPopLeft(CacheConstants.ANALYTICS_BUFFER_KEY, bufferSize,
				VisitLog.class);
		List<VisitLog> logsToPersist = bufferedLogs.stream()
				.filter(log -> StringUtils.hasText(log.getVisitorHash()) && StringUtils.hasText(log.getSessionId())).toList();
		if (logsToPersist.size() != bufferedLogs.size()) {
			log.warn("Discarded {} legacy analytics buffer entries without anonymous identifiers",
					bufferedLogs.size() - logsToPersist.size());
		}

		if (!logsToPersist.isEmpty()) {
			// Process in sub-batches if the total size is very large
			int total = logsToPersist.size();
			for (int i = 0; i < total; i += BATCH_SIZE) {
				int end = Math.min(i + BATCH_SIZE, total);
				visitLogRepository.saveAll(logsToPersist.subList(i, end));
			}
			log.info("Successfully persisted {} analytics logs to database.", total);
		}
	}
}
