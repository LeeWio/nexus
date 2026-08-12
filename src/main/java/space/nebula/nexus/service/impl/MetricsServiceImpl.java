package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Dict;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.stereotype.Service;
import space.nebula.nexus.service.IMetricsService;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements IMetricsService {

	private final MeterRegistry meterRegistry;
	private final CaffeineCacheManager caffeineCacheManager;

	@Override
	public Dict getSystemPerformanceSnapshot() {
		return Dict.create().set("jvm", getJvmMetrics()).set("http", getHttpMetrics()).set("cache", getCacheMetrics())
				.set("mq", getMqMetrics()).set("timestamp", System.currentTimeMillis());
	}

	private Dict getJvmMetrics() {
		double memoryUsed = meterRegistry.get("jvm.memory.used").gauge().value();
		double memoryMax = meterRegistry.get("jvm.memory.max").gauge().value();
		double threads = meterRegistry.get("jvm.threads.live").gauge().value();

		return Dict.create().set("memoryUsedMb", Math.round(memoryUsed / 1024 / 1024))
				.set("memoryMaxMb", Math.round(memoryMax / 1024 / 1024))
				.set("memoryUsageRatio", Math.round((memoryUsed / memoryMax) * 100)).set("liveThreads", (int) threads);
	}

	private Dict getHttpMetrics() {
		Search search = meterRegistry.find("http.server.requests");
		if (search.timer() == null) {
			return Dict.create().set("totalRequests", 0).set("avgResponseTimeMs", 0);
		}

		long totalCount = search.timers().stream().mapToLong(Timer::count).sum();
		double avgTime = search.timers().stream().mapToDouble(t -> t.mean(TimeUnit.MILLISECONDS)).average().orElse(0.0);

		return Dict.create().set("totalRequests", totalCount).set("avgResponseTimeMs",
				Math.round(avgTime * 100.0) / 100.0);
	}

	private Dict getCacheMetrics() {
		Collection<String> cacheNames = caffeineCacheManager.getCacheNames();
		long totalHits = 0;
		long totalMisses = 0;

		for (String name : cacheNames) {
			CaffeineCache cache = (CaffeineCache) caffeineCacheManager.getCache(name);
			if (cache != null) {
				var stats = cache.getNativeCache().stats();
				totalHits += stats.hitCount();
				totalMisses += stats.missCount();
			}
		}

		double hitRate = (totalHits + totalMisses == 0) ? 0.0 : (double) totalHits / (totalHits + totalMisses);

		return Dict.create().set("l1HitCount", totalHits).set("l1MissCount", totalMisses).set("l1HitRate",
				Math.round(hitRate * 10000.0) / 100.0); //e.g. 95.55
	}

	private Dict getMqMetrics() {
		Search search = meterRegistry.find("nexus.mq.canal.processing");

		long processed = search.timers().stream().mapToLong(Timer::count).sum();
		long errors = search.tag("status", "error").timers().stream().mapToLong(Timer::count).sum();

		return Dict.create().set("canalMessagesProcessed", processed).set("canalProcessingErrors", errors);
	}
}
