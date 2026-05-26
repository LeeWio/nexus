package space.nebula.nexus.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.payload.request.CanalMessage;
import space.nebula.nexus.utils.RedisUtil;

import java.util.Map;

/**
 * Asynchronous cache invalidation listener using Canal Binlog events via
 * RabbitMQ. Decouples cache logic from core business services.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanalCacheInvalidationListener
{

	private final RedisUtil redisUtil;

	@RabbitListener(queues = RabbitMQConfig.CANAL_QUEUE)
	public void processBinlogMessage(CanalMessage message)
	{
		if (message == null || StrUtil.isBlank(message.getTable()))
		{
			return;
		}

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
			// Add other tables as needed
			default:
				break;
			}
		}
		catch (Exception e)
		{
			log.error("Failed to process cache invalidation for table: {}", table, e);
		}
	}

	private void handlePostCacheInvalidation(CanalMessage message)
	{
		// Clear global list caches
		redisUtil.deleteByPattern(CacheConstants.buildFullKey(CacheConstants.BLOG_POSTS, "list:*"));
		redisUtil.delete(CacheConstants.buildFullKey(CacheConstants.SEO, CacheConstants.SITEMAP_KEY));
		redisUtil.delete(CacheConstants.buildFullKey(CacheConstants.SEO, CacheConstants.RSS_FEED_KEY));

		// If it's an UPDATE or DELETE, we can try to be more surgical if we have the
		// slug
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

		// Also check 'old' for changed slugs
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

		log.info("Canal cleared Post caches automatically.");
	}

	private void handleMenuCacheInvalidation()
	{
		redisUtil.deleteByPattern(CacheConstants.NAVIGATION + "*");
		log.info("Canal cleared Menu caches automatically.");
	}

	private void handleConfigCacheInvalidation()
	{
		redisUtil.deleteByPattern("nexus:cache:sys_config:*"); // Assuming default Spring cache prefix
		log.info("Canal cleared System Config caches automatically.");
	}
}
