package space.nebula.nexus.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.cache.RedisCache;

import java.util.concurrent.Callable;

/**
 * Custom Multi-Level Cache implementation (L1 Caffeine + L2 Redis).
 */
@Slf4j
public class MultiLevelCache implements Cache {

    private final String name;
    private final Cache l1Cache;
    private final RedisCache l2Cache;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final String instanceId;
    private final String topic;

    public MultiLevelCache(String name, Cache l1Cache, RedisCache l2Cache, 
                           org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate, 
                           String instanceId, String topic) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.redisTemplate = redisTemplate;
        this.instanceId = instanceId;
        this.topic = topic;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        // 1. Try L1
        ValueWrapper wrapper = l1Cache.get(key);
        if (wrapper != null) {
            log.trace("L1 Cache Hit: {}::{}", name, key);
            return wrapper;
        }

        // 2. Try L2
        wrapper = l2Cache.get(key);
        if (wrapper != null) {
            log.trace("L1 Cache Miss, L2 Cache Hit: {}::{}", name, key);
            // Backfill L1
            l1Cache.put(key, wrapper.get());
            return wrapper;
        }

        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = l1Cache.get(key, type);
        if (value != null) return value;

        value = l2Cache.get(key, type);
        if (value != null) {
            l1Cache.put(key, value);
        }
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }

        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        l1Cache.put(key, value);
        l2Cache.put(key, value);
        publishMessage(key);
    }

    @Override
    public void evict(Object key) {
        l1Cache.evict(key);
        l2Cache.evict(key);
        publishMessage(key);
    }

    @Override
    public void clear() {
        l1Cache.clear();
        l2Cache.clear();
        publishMessage(null);
    }

    private void publishMessage(Object key) {
        redisTemplate.convertAndSend(topic, new CacheMessage(name, key, instanceId));
    }
}
