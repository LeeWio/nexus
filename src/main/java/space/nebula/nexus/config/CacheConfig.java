package space.nebula.nexus.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import space.nebula.nexus.common.cache.CacheMessageListener;
import space.nebula.nexus.common.cache.MultiLevelCacheManager;

import java.util.concurrent.TimeUnit;

@Configuration
@RequiredArgsConstructor
public class CacheConfig
{

	private static final String CACHE_TOPIC = "nexus:cache:invalidation";
	private final String instanceId = cn.hutool.core.util.IdUtil.fastSimpleUUID();

	/**
	 * Local L1 Cache Manager (Caffeine).
	 */
	@Bean
	public CaffeineCacheManager caffeineCacheManager()
	{
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder().initialCapacity(100).maximumSize(500)
				.expireAfterWrite(10, TimeUnit.MINUTES).recordStats();

		cacheManager.setCaffeine(caffeineBuilder);
		return cacheManager;
	}

	/**
	 * Primary Cache Manager using Multi-Level Strategy (L1 Caffeine + L2 Redis).
	 */
	@Bean
	@Primary
	public CacheManager multiLevelCacheManager(CaffeineCacheManager caffeineCacheManager,
			RedisCacheManager redisCacheManager, RedisTemplate<String, Object> redisTemplate)
	{
		return new MultiLevelCacheManager(caffeineCacheManager, redisCacheManager, redisTemplate, instanceId,
				CACHE_TOPIC);
	}

	/**
	 * Redis Message Listener for L1 cache invalidation across multiple instances.
	 */
	@Bean
	@ConditionalOnProperty(name = "app.cache.invalidation-listener-enabled", havingValue = "true", matchIfMissing = true)
	public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory factory,
			CacheMessageListener listener)
	{
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.addMessageListener(listener, new ChannelTopic(CACHE_TOPIC));
		return container;
	}

	@Bean
	public CacheMessageListener cacheMessageListener(RedisTemplate<String, Object> redisTemplate,
			CaffeineCacheManager caffeineCacheManager)
	{
		return new CacheMessageListener(redisTemplate, caffeineCacheManager, instanceId);
	}
}
