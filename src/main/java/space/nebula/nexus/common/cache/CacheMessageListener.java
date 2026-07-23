package space.nebula.nexus.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class CacheMessageListener implements MessageListener {

	private final RedisTemplate<String, Object> redisTemplate;
	private final CaffeineCacheManager caffeineCacheManager;
	private final String instanceId;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		CacheMessage cacheMessage = (CacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
		if (cacheMessage == null || Objects.equals(cacheMessage.getSourceInstanceId(), instanceId)) {
			return;
		}

		log.debug("Received cache invalidation message for {}:{}", cacheMessage.getCacheName(), cacheMessage.getKey());
		var cache = caffeineCacheManager.getCache(cacheMessage.getCacheName());
		if (cache != null) {
			if (cacheMessage.getKey() == null) {
				cache.clear();
			} else {
				cache.evict(cacheMessage.getKey());
			}
		}
	}
}
