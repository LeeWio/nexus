package space.nebula.nexus.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Utility for Redis distributed locking.
 */
@Component
@Slf4j
public class RedisLockUtil
{

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private static final String RELEASE_LOCK_LUA_SCRIPT = "if redis.call('get', KEYS[1]) == ARGV[1] then "
			+ "return redis.call('del', KEYS[1]) " + "else return 0 end";

	/**
	 * Try to acquire a lock.
	 * 
	 * @param key     Lock key
	 * @param value   Lock value (unique ID like request ID or thread name)
	 * @param timeout Expiration time
	 * @param unit    Time unit
	 * @return true if acquired
	 */
	public boolean tryLock(String key, String value, long timeout, TimeUnit unit)
	{
		try
		{
			Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
			return Boolean.TRUE.equals(result);
		}
		catch (Exception e)
		{
			log.error("Error acquiring lock for key: {}", key, e);
			return false;
		}
	}

	/**
	 * Release a lock safely using Lua script to ensure only the owner releases it.
	 * 
	 * @param key   Lock key
	 * @param value Lock value (must match the one used to acquire)
	 * @return true if released
	 */
	public boolean unlock(String key, String value)
	{
		try
		{
			DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_LOCK_LUA_SCRIPT, Long.class);
			Long result = stringRedisTemplate.execute(script, Collections.singletonList(key), value);
			return result != null && result > 0;
		}
		catch (Exception e)
		{
			log.error("Error releasing lock for key: {}", key, e);
			return false;
		}
	}
}
