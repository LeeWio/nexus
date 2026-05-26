package space.nebula.nexus.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.event.ConfigChangedEvent;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.payload.request.CanalMessage;
import space.nebula.nexus.payload.request.L1CacheInvalidationMessage;
import space.nebula.nexus.utils.RedisUtil;

import java.util.Map;

/**
 * Asynchronous cache invalidation listener using Canal Binlog events via
 * RabbitMQ. Implements L1+L2 consistency by broadcasting to all instances.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanalCacheInvalidationListener
{

	private final RedisUtil redisUtil;
	private final RabbitTemplate rabbitTemplate;
	private final CacheManager caffeineCacheManager;
	private final MeterRegistry meterRegistry;

	@RabbitListener(queues = RabbitMQConfig.CANAL_QUEUE)
	public void processBinlogMessage(CanalMessage message)
	{
		if (ObjectUtil.isNull(message) || StrUtil.isBlank(message.getTable()))
		{
			return;
		}

		Timer.Sample timer = Timer.start(meterRegistry);
		String table = message.getTable();
		String type = message.getType();

		log.debug("Received Canal Event - Table: {}, Type: {}", table, type);

		try
		{
			switch (table) {
			case "blog_post":
				handlePostCacheInvalidation(message);
				break;
			case "blog_menu":
				handleMenuCacheInvalidation();
				break;
			case "blog_config":
				handleConfigCacheInvalidation();
				break;
			default:
				break;
			}
			timer.stop(meterRegistry.timer("nexus.mq.canal.processing", "table", table, "type", type, "status",
					"success"));
		}
		catch (Exception e)
		{
			timer.stop(
					meterRegistry.timer("nexus.mq.canal.processing", "table", table, "type", type, "status", "error"));
			log.error("Failed to process cache invalidation for table: {}", table, e);
		}
	}

	/**
	 * Listens to internal config change events and syncs cluster caches.
	 */
	@Async("asyncExecutor")
	@EventListener
	public void handleConfigChanged(ConfigChangedEvent event)
	{
		log.info("Internal config change detected for key: {}. Syncing cluster caches...", event.getConfigKey());

		// 1. Clear L2 (Redis) - specific key or public list
		redisUtil.delete(CacheConstants.buildFullKey(CacheConstants.SYS_CONFIG, event.getConfigKey()));
		redisUtil.delete(CacheConstants.buildFullKey(CacheConstants.SYS_CONFIG, CacheConstants.PUBLIC_CONFIGS_KEY));

		// 2. Broadcast L1 (Caffeine)
		broadcastL1Invalidation(CacheConstants.SYS_CONFIG, event.getConfigKey(), false);
		broadcastL1Invalidation(CacheConstants.SYS_CONFIG, CacheConstants.PUBLIC_CONFIGS_KEY, false);
	}

	/**
	 * Listens to L1 invalidation broadcasts (from this or other instances).
	 */
	@RabbitListener(queues = "#{l1CacheInvalidationQueue.name}")
	public void processL1Invalidation(L1CacheInvalidationMessage message)
	{
		if (ObjectUtil.isNull(message))
			return;

		var cache = caffeineCacheManager.getCache(message.getCacheName());
		if (ObjectUtil.isNull(cache))
			return;

		if (message.isClearAll())
		{
			cache.clear();
			log.debug("L1 Cache cleared (all): {}", message.getCacheName());
		}
		else if (StrUtil.isNotBlank(message.getKey()))
		{
			cache.evict(message.getKey());
			log.debug("L1 Cache evicted: {} -> {}", message.getCacheName(), message.getKey());
		}
	}

	private void handlePostCacheInvalidation(CanalMessage message)
	{
		// 1. Clear L2 (Redis)
		redisUtil.deleteByPattern(CacheConstants.buildFullKey(CacheConstants.BLOG_POSTS, "list:*"));
		redisUtil.delete(CacheConstants.buildFullKey(CacheConstants.SEO, CacheConstants.SITEMAP_KEY));
		redisUtil.delete(CacheConstants.buildFullKey(CacheConstants.SEO, CacheConstants.RSS_FEED_KEY));

		// 2. Broadcast L1 (Caffeine) Invalidation
		broadcastL1Invalidation(CacheConstants.BLOG_POSTS, null, true);

		if (CollUtil.isNotEmpty(message.getData()))
		{
			for (Map<String, String> row : message.getData())
			{
				String slug = row.get("slug");
				if (StrUtil.isNotBlank(slug))
				{
					redisUtil.delete(CacheConstants.POST_SLUG_PREFIX + slug);
				}
			}
		}

		if (CollUtil.isNotEmpty(message.getOld()))
		{
			for (Map<String, String> row : message.getOld())
			{
				String oldSlug = row.get("slug");
				if (StrUtil.isNotBlank(oldSlug))
				{
					redisUtil.delete(CacheConstants.POST_SLUG_PREFIX + oldSlug);
				}
			}
		}

		log.info("Canal cleared Post L1+L2 caches.");
	}

	private void handleMenuCacheInvalidation()
	{
		redisUtil.deleteByPattern(CacheConstants.NAVIGATION + "*");
		broadcastL1Invalidation(CacheConstants.NAVIGATION, null, true);
		log.info("Canal cleared Menu L1+L2 caches.");
	}

	private void handleConfigCacheInvalidation()
	{
		redisUtil.deleteByPattern("nexus:cache:sys_config:*");
		broadcastL1Invalidation("sys_config", null, true);
		log.info("Canal cleared System Config L1+L2 caches.");
	}

	private void broadcastL1Invalidation(String cacheName, String key, boolean clearAll)
	{
		L1CacheInvalidationMessage broadcast = L1CacheInvalidationMessage.builder().cacheName(cacheName).key(key)
				.clearAll(clearAll).build();
		rabbitTemplate.convertAndSend(RabbitMQConfig.CACHE_BROADCAST_EXCHANGE, "", broadcast);
	}
}
