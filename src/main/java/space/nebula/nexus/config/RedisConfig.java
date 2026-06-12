package space.nebula.nexus.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig
{

	@Bean
	RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory)
	{
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);

		template.setKeySerializer(RedisSerializer.string());
		template.setHashKeySerializer(RedisSerializer.string());

		template.setValueSerializer(RedisSerializer.json());
		template.setHashValueSerializer(RedisSerializer.json());

		template.afterPropertiesSet();
		return template;
	}

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory factory)
	{
		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(1))
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()))
				.disableCachingNullValues();

		// Refined TTLs for different cache regions
		java.util.Map<String, RedisCacheConfiguration> initialConfigurations = new java.util.HashMap<>();
		
		// Long-term caches (12 hours)
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.CATEGORIES, config.entryTtl(Duration.ofHours(12)));
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.TAGS, config.entryTtl(Duration.ofHours(12)));
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.SYS_CONFIG, config.entryTtl(Duration.ofHours(12)));
		
		// Mid-term caches (6 hours)
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.NAVIGATION, config.entryTtl(Duration.ofHours(6)));
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.SEO, config.entryTtl(Duration.ofHours(6)));
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.PROJECTS, config.entryTtl(Duration.ofHours(6)));
		
		// Short-term caches (10 minutes)
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.ANALYTICS, config.entryTtl(Duration.ofMinutes(10)));
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.SITE_STATS, config.entryTtl(Duration.ofMinutes(10)));
		initialConfigurations.put(space.nebula.nexus.common.constant.CacheConstants.MARKET_INDICES, config.entryTtl(Duration.ofMinutes(10)));

		return RedisCacheManager.builder(factory)
				.cacheDefaults(config)
				.withInitialCacheConfigurations(initialConfigurations)
				.build();
	}
}
