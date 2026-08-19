package space.nebula.nexus.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.LinkCheckLog;
import space.nebula.nexus.repository.LinkCheckLogRepository;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persists one link-health page with a bounded number of database round trips.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkHealthLogPersistenceService {

	private final LinkCheckLogRepository linkCheckLogRepository;

	public record LinkCheckLogUpdate(String url, String sourceType, Long sourceId, String sourceTitle, Integer status,
			boolean isBroken, String error) {
	}

	private record LogKey(String url, String sourceType, Long sourceId) {

		private static LogKey from(LinkCheckLogUpdate update) {
			return new LogKey(update.url(), update.sourceType(), update.sourceId());
		}

		private static LogKey from(LinkCheckLog log) {
			return new LogKey(log.getUrl(), log.getSourceType(), log.getSourceId());
		}
	}

	/**
	 * Fetches all existing logs for a page before saving its updates. This replaces
	 * one lookup and transaction per result with one lookup per source type and a
	 * single write transaction.
	 */
	@Transactional
	public void saveBatch(List<LinkCheckLogUpdate> updates) {
		if (updates.isEmpty()) {
			return;
		}

		Map<LogKey, LinkCheckLog> existingLogs = findExistingLogs(updates);
		Collection<LinkCheckLogUpdate> distinctUpdates = distinctByLogKey(updates).values();
		List<LinkCheckLog> logs = distinctUpdates.stream()
				.map(update -> applyUpdate(existingLogs.getOrDefault(LogKey.from(update), new LinkCheckLog()), update))
				.toList();

		linkCheckLogRepository.saveAll(logs);
		log.info("Persisted {} link health check logs", logs.size());
		logs.stream().filter(logEntry -> Boolean.TRUE.equals(logEntry.getIsBroken()))
				.forEach(logEntry -> log.warn("Broken link detected in {}: {} -> {}", logEntry.getSourceType(),
						logEntry.getSourceTitle(), logEntry.getUrl()));
	}

	private Map<LogKey, LinkCheckLog> findExistingLogs(List<LinkCheckLogUpdate> updates) {
		Map<String, Set<Long>> sourceIdsByType = updates.stream().filter(update -> update.sourceId() != null)
				.collect(Collectors.groupingBy(LinkCheckLogUpdate::sourceType,
						Collectors.mapping(LinkCheckLogUpdate::sourceId, Collectors.toSet())));

		Map<LogKey, LinkCheckLog> existingLogs = new HashMap<>();
		sourceIdsByType.forEach(
				(sourceType, sourceIds) -> linkCheckLogRepository.findBySourceTypeAndSourceIdIn(sourceType, sourceIds)
						.forEach(log -> existingLogs.putIfAbsent(LogKey.from(log), log)));
		return existingLogs;
	}

	private Map<LogKey, LinkCheckLogUpdate> distinctByLogKey(List<LinkCheckLogUpdate> updates) {
		Map<LogKey, LinkCheckLogUpdate> distinctUpdates = new LinkedHashMap<>();
		updates.forEach(update -> distinctUpdates.put(LogKey.from(update), update));
		return distinctUpdates;
	}

	private LinkCheckLog applyUpdate(LinkCheckLog logEntry, LinkCheckLogUpdate update) {
		logEntry.setUrl(update.url());
		logEntry.setSourceType(update.sourceType());
		logEntry.setSourceId(update.sourceId());
		logEntry.setSourceTitle(update.sourceTitle());
		logEntry.setStatusCode(update.status());
		logEntry.setIsBroken(update.isBroken());
		logEntry.setErrorMessage(StrUtil.maxLength(update.error(), 450));
		return logEntry;
	}
}
