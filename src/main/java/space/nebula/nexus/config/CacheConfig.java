package space.nebula.nexus.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class CacheConfig
{

	/**
	 * Local L1 Cache Manager (Caffeine). Fast, but not distributed.
	 */
	@Bean
	CacheManager caffeineCacheManager()
	{
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder().initialCapacity(100).maximumSize(500)
				.expireAfterWrite(10, TimeUnit.MINUTES).recordStats();

		cacheManager.setCaffeine(caffeineBuilder);

		// Note: CaffeineCacheManager creates caches on demand.
		// For monitoring, we might want to pre-define some or use a custom decorator.
		// However, Micrometer's CaffeineCacheMetrics can bind to a cache instance.
		return cacheManager;
	}
}
