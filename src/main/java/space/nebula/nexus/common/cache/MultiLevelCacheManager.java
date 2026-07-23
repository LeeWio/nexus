package space.nebula.nexus.common.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Custom CacheManager for Multi-Level Caching.
 */
public class MultiLevelCacheManager implements CacheManager {

	private final CaffeineCacheManager caffeineCacheManager;
	private final RedisCacheManager redisCacheManager;
	private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
	private final String instanceId;
	private final String topic;
	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

	public MultiLevelCacheManager(CaffeineCacheManager caffeineCacheManager, RedisCacheManager redisCacheManager,
			org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate, String instanceId,
			String topic) {
		this.caffeineCacheManager = caffeineCacheManager;
		this.redisCacheManager = redisCacheManager;
		this.redisTemplate = redisTemplate;
		this.instanceId = instanceId;
		this.topic = topic;
	}

	@Override
	public Cache getCache(String name) {
		return caches.computeIfAbsent(name, k -> {
			Cache l1 = caffeineCacheManager.getCache(k);
			RedisCache l2 = (RedisCache) redisCacheManager.getCache(k);
			return new MultiLevelCache(k, l1, l2, redisTemplate, instanceId, topic);
		});
	}

	@Override
	public Collection<String> getCacheNames() {
		return Collections.unmodifiableSet(caches.keySet());
	}
}
