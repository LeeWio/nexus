package space.nebula.nexus.utils;

import cn.hutool.core.collection.CollUtil;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced Redis utility class with additional functionality. This utility
 * extends the basic Redis operations with more advanced features such as list
 * operations, set operations, and hash operations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RedisUtil
{
	private static final RedisScript<Long> COMPARE_AND_DELETE_SCRIPT = RedisScript.of(
			"if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
			Long.class);

	private final RedisTemplate<String, Object> redisTemplate;

	// ==================== Basic Operations ====================

	/**
	 * Sets a key-value pair with no expiration.
	 *
	 * @param key   the key
	 * @param value the value
	 * @return true if successful, false otherwise
	 */
	public <T> boolean set(String key, T value)
	{
		try
		{
			redisTemplate.opsForValue().set(key, value);
			return true;
		}
		catch (Exception e)
		{
			log.error("Error setting value for key: {}", key, e);
			return false;
		}
	}

	/**
	 * Sets a key-value pair with an expiration time.
	 *
	 * @param key     the key
	 * @param value   the value
	 * @param timeout the time to live
	 * @param unit    the time unit
	 * @return true if successful, false otherwise
	 */
	public <T> boolean set(String key, T value, long timeout, TimeUnit unit)
	{
		try
		{
			redisTemplate.opsForValue().set(key, value, timeout, unit);
			return true;
		}
		catch (Exception e)
		{
			log.error("Error setting value for key: {}", key, e);
			return false;
		}
	}

	/**
	 * Gets a value by key and converts it to the specified class.
	 *
	 * @param key   the key
	 * @param clazz the expected class type
	 * @return Optional containing the value if found and of correct type
	 */
	public <T> Optional<T> get(String key, Class<T> clazz)
	{
		try
		{
			Object value = redisTemplate.opsForValue().get(key);

			if (value == null || !clazz.isInstance(value))
			{
				return Optional.empty();
			}
			else
			{
				return Optional.of(clazz.cast(value));
			}
		}
		catch (Exception e)
		{
			log.error("Error getting value for key: {}", key, e);
			return Optional.empty();
		}
	}

	/**
	 * Atomically deletes a key only when its current value equals the expected value.
	 *
	 * @param key the Redis key
	 * @param expectedValue the value that must currently be stored
	 * @return {@code true} when the value matched and the key was deleted
	 */
	public boolean consumeIfEquals(String key, String expectedValue)
	{
		try
		{
			Long result = redisTemplate.execute(COMPARE_AND_DELETE_SCRIPT, List.of(key), expectedValue);
			return result != null && result == 1L;
		}
		catch (Exception e)
		{
			log.error("Error atomically consuming value for key: {}", key, e);
			return false;
		}
	}

	/**
	 * Increments a numeric value by 1.
	 *
	 * @param key the key
	 */
	public void increment(String key)
	{
		try
		{
			redisTemplate.opsForValue().increment(key);
		}
		catch (Exception e)
		{
			log.error("Error incrementing value for key: {}", key, e);
		}
	}

	/**
	 * Increments a numeric value by the specified delta.
	 *
	 * @param key   the key
	 * @param delta the increment value
	 * @return the new value after increment
	 */
	public Long increment(String key, long delta)
	{
		try
		{
			return redisTemplate.opsForValue().increment(key, delta);
		}
		catch (Exception e)
		{
			log.error("Error incrementing value for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Decrements a numeric value by 1.
	 *
	 * @param key the key
	 */
	public void decrement(String key)
	{
		try
		{
			redisTemplate.opsForValue().increment(key, -1);
		}
		catch (Exception e)
		{
			log.error("Error decrementing value for key: {}", key, e);
		}
	}

	/**
	 * Decrements a numeric value by the specified delta.
	 *
	 * @param key   the key
	 * @param delta the decrement value
	 * @return the new value after decrement
	 */
	public Long decrement(String key, long delta)
	{
		try
		{
			return redisTemplate.opsForValue().increment(key, -delta);
		}
		catch (Exception e)
		{
			log.error("Error decrementing value for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Sets an expiration time for an existing key.
	 *
	 * @param key     the key
	 * @param timeout the time to live
	 * @param unit    the time unit
	 * @return true if successful, false otherwise
	 */
	public boolean expire(String key, long timeout, TimeUnit unit)
	{
		try
		{
			return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Gets the time to live for a key in seconds.
	 *
	 * @param key the key
	 * @return time to live in seconds, -2 if key doesn't exist, -1 if no expiration
	 */
	public Long getExpire(String key)
	{
		try
		{
			return redisTemplate.getExpire(key, TimeUnit.SECONDS);
		}
		catch (Exception e)
		{
			log.error("Error getting expire time for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Checks if a key exists.
	 *
	 * @param key the key
	 * @return true if key exists, false otherwise
	 */
	public boolean hasKey(String key)
	{
		try
		{
			return Boolean.TRUE.equals(redisTemplate.hasKey(key));
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Deletes a single key.
	 *
	 * @param key the key to delete
	 */
	public void delete(String key)
	{
		try
		{
			redisTemplate.delete(key);
		}
		catch (Exception e)
		{
			log.error("Error deleting key: {}", key, e);
		}
	}

	/**
	 * Renames a key.
	 *
	 * @param oldKey the old key
	 * @param newKey the new key
	 * @return true if successful, false otherwise
	 */
	public boolean rename(String oldKey, String newKey)
	{
		try
		{
			redisTemplate.rename(oldKey, newKey);
			return true;
		}
		catch (Exception e)
		{
			log.error("Error renaming key {} to {}: {}", oldKey, newKey, e.getMessage());
			return false;
		}
	}

	/**
	 * Deletes multiple keys.
	 *
	 * @param keys the collection of keys to delete
	 */
	public void delete(Collection<String> keys)
	{
		try
		{
			redisTemplate.delete(keys);
		}
		catch (Exception e)
		{
			log.error("Error deleting multiple keys", e);
		}
	}

	/**
	 * Deletes all keys matching the specified pattern.
	 *
	 * @param pattern the pattern to match (e.g., "nexus:cache:*")
	 */
	public void deleteByPattern(String pattern)
	{
		try
		{
			Set<String> keys = redisTemplate.keys(pattern);
			if (CollUtil.isNotEmpty(keys))
			{
				redisTemplate.delete(keys);
				log.debug("Deleted {} keys matching pattern: {}", keys.size(), pattern);
			}
		}
		catch (Exception e)
		{
			log.error("Error deleting keys by pattern: {}", pattern, e);
		}
	}

	/**
	 * Acquires a distributed lock using SETNX.
	 *
	 * @param key     the lock key
	 * @param value   the lock value (e.g., node identifier)
	 * @param timeout the time to live for the lock
	 * @param unit    the time unit
	 * @return true if the lock was acquired, false otherwise
	 */
	public boolean lock(String key, String value, long timeout, TimeUnit unit)
	{
		try
		{
			return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit));
		}
		catch (Exception e)
		{
			log.error("Error acquiring lock for key: {}", key, e);
			return false;
		}
	}

	/**
	 * Releases a distributed lock.
	 *
	 * @param key the lock key
	 */
	public void unlock(String key)
	{
		try
		{
			redisTemplate.delete(key);
		}
		catch (Exception e)
		{
			log.error("Error releasing lock for key: {}", key, e);
		}
	}

	// ==================== List Operations ====================

	/**
	 * Adds an element to the end of a list.
	 *
	 * @param key   the key
	 * @param value the value to add
	 * @return the length of the list after the push operation
	 */
	public Long listAdd(String key, Object value)
	{
		try
		{
			return redisTemplate.opsForList().rightPush(key, value);
		}
		catch (Exception e)
		{
			log.error("Error adding to list for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Adds multiple elements to the end of a list.
	 *
	 * @param key    the key
	 * @param values the values to add
	 * @return the length of the list after the push operation
	 */
	public Long listAddAll(String key, Object... values)
	{
		try
		{
			return redisTemplate.opsForList().rightPushAll(key, values);
		}
		catch (Exception e)
		{
			log.error("Error adding multiple elements to list for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Gets elements from a list by range.
	 *
	 * @param key   the key
	 * @param start the starting index
	 * @param end   the ending index (use -1 for the end of the list)
	 * @return the list of elements
	 */
	public List<Object> listRange(String key, long start, long end)
	{
		try
		{
			return redisTemplate.opsForList().range(key, start, end);
		}
		catch (Exception e)
		{
			log.error("Error getting list range for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Gets the size of a list.
	 *
	 * @param key the key
	 * @return the size of the list
	 */
	public Long listSize(String key)
	{
		try
		{
			return redisTemplate.opsForList().size(key);
		}
		catch (Exception e)
		{
			log.error("Error getting list size for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Removes elements from a list by value.
	 *
	 * @param key   the key
	 * @param count the count of elements to remove (0 for all, positive for first
	 *              occurrences, negative for last occurrences)
	 * @param value the value to remove
	 * @return the number of removed elements
	 */
	public Long listRemove(String key, long count, Object value)
	{
		try
		{
			return redisTemplate.opsForList().remove(key, count, value);
		}
		catch (Exception e)
		{
			log.error("Error removing from list for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Removes and returns the first element of the list.
	 *
	 * @param key   the key
	 * @param clazz the expected class type
	 * @return Optional containing the popped value if found and of correct type
	 */
	public <T> Optional<T> getAndPopLeft(String key, Class<T> clazz)
	{
		try
		{
			Object value = redisTemplate.opsForList().leftPop(key);
			if (value == null || !clazz.isInstance(value))
			{
				return Optional.empty();
			}
			else
			{
				return Optional.of(clazz.cast(value));
			}
		}
		catch (Exception e)
		{
			log.error("Error popping from list for key: {}", key, e);
			return Optional.empty();
		}
	}

	/**
	 * Removes and returns multiple elements from the beginning of the list.
	 *
	 * @param key   the key
	 * @param count the number of elements to pop
	 * @param clazz the expected class type
	 * @return the list of popped elements
	 */
	public <T> List<T> listPopLeft(String key, long count, Class<T> clazz)
	{
		try
		{
			List<Object> values = redisTemplate.opsForList().leftPop(key, count);
			if (values == null)
				return java.util.Collections.emptyList();
			return values.stream().filter(clazz::isInstance).map(clazz::cast).toList();
		}
		catch (Exception e)
		{
			log.error("Error popping multiple from list for key: {}", key, e);
			return java.util.Collections.emptyList();
		}
	}

	/**
	 * Increments the value of a hash field by the specified delta. ...
	 * 
	 * @param key     the key
	 * @param hashKey the hash key
	 * @param delta   the increment value
	 * @return the new value after increment
	 */
	public Long hashIncrement(String key, String hashKey, long delta)
	{
		try
		{
			return redisTemplate.opsForHash().increment(key, hashKey, delta);
		}
		catch (Exception e)
		{
			log.error("Error incrementing hash field for key: {}, hashKey: {}", key, hashKey, e);
			return null;
		}
	}

	// ==================== Set Operations ====================

	/**
	 * Adds an element to a set.
	 *
	 * @param key   the key
	 * @param value the value to add
	 * @return true if the element was added, false if it was already in the set
	 */
	public Boolean setAdd(String key, Object... values)
	{
		try
		{
			Long result = redisTemplate.opsForSet().add(key, values);
			return result != null && result > 0;
		}
		catch (Exception e)
		{
			log.error("Error adding to set for key: {}", key, e);
			return false;
		}
	}

	/**
	 * Gets all members of a set.
	 *
	 * @param key the key
	 * @return the set of members
	 */
	public Set<Object> setMembers(String key)
	{
		try
		{
			return redisTemplate.opsForSet().members(key);
		}
		catch (Exception e)
		{
			log.error("Error getting set members for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Checks if a value is a member of a set.
	 *
	 * @param key   the key
	 * @param value the value to check
	 * @return true if the value is in the set, false otherwise
	 */
	public Boolean setIsMember(String key, Object value)
	{
		try
		{
			return redisTemplate.opsForSet().isMember(key, value);
		}
		catch (Exception e)
		{
			log.error("Error checking set member for key: {}", key, e);
			return false;
		}
	}

	/**
	 * Removes elements from a set.
	 *
	 * @param key    the key
	 * @param values the values to remove
	 * @return the number of removed elements
	 */
	public Long setRemove(String key, Object... values)
	{
		try
		{
			return redisTemplate.opsForSet().remove(key, values);
		}
		catch (Exception e)
		{
			log.error("Error removing from set for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Gets the size of a set.
	 *
	 * @param key the key
	 * @return the size of the set
	 */
	public Long setSize(String key)
	{
		try
		{
			return redisTemplate.opsForSet().size(key);
		}
		catch (Exception e)
		{
			log.error("Error getting set size for key: {}", key, e);
			return null;
		}
	}

	// ==================== Hash Operations ====================

	/**
	 * Sets a field in a hash.
	 *
	 * @param key     the key
	 * @param hashKey the hash key
	 * @param value   the value
	 */
	public void hashPut(String key, String hashKey, Object value)
	{
		try
		{
			redisTemplate.opsForHash().put(key, hashKey, value);
		}
		catch (Exception e)
		{
			log.error("Error putting hash field for key: {}, hashKey: {}", key, hashKey, e);
		}
	}

	/**
	 * Sets multiple fields in a hash.
	 *
	 * @param key the key
	 * @param map the map of hash key-value pairs
	 */
	public void hashPutAll(String key, java.util.Map<String, Object> map)
	{
		try
		{
			redisTemplate.opsForHash().putAll(key, map);
		}
		catch (Exception e)
		{
			log.error("Error putting all hash fields for key: {}", key, e);
		}
	}

	/**
	 * Gets a field from a hash.
	 *
	 * @param key     the key
	 * @param hashKey the hash key
	 * @return the value of the field
	 */
	public Object hashGet(String key, String hashKey)
	{
		try
		{
			return redisTemplate.opsForHash().get(key, hashKey);
		}
		catch (Exception e)
		{
			log.error("Error getting hash field for key: {}, hashKey: {}", key, hashKey, e);
			return null;
		}
	}

	/**
	 * Gets multiple fields from a hash.
	 *
	 * @param key      the key
	 * @param hashKeys the hash keys
	 * @return the map of hash key-value pairs
	 */
	public java.util.Map<Object, Object> hashGetAll(String key)
	{
		try
		{
			return redisTemplate.opsForHash().entries(key);
		}
		catch (Exception e)
		{
			log.error("Error getting all hash fields for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Checks if a field exists in a hash.
	 *
	 * @param key     the key
	 * @param hashKey the hash key
	 * @return true if the field exists, false otherwise
	 */
	public Boolean hashHasKey(String key, String hashKey)
	{
		try
		{
			return redisTemplate.opsForHash().hasKey(key, hashKey);
		}
		catch (Exception e)
		{
			log.error("Error checking hash key for key: {}, hashKey: {}", key, hashKey, e);
			return false;
		}
	}

	/**
	 * Removes fields from a hash.
	 *
	 * @param key      the key
	 * @param hashKeys the hash keys to remove
	 * @return the number of removed fields
	 */
	public Long hashDelete(String key, Object... hashKeys)
	{
		try
		{
			return redisTemplate.opsForHash().delete(key, hashKeys);
		}
		catch (Exception e)
		{
			log.error("Error deleting hash fields for key: {}", key, e);
			return null;
		}
	}

	/**
	 * Atomically reads all fields from a hash and then deletes the key using a Lua
	 * script.
	 * 
	 * @param key the key
	 * @return the map of hash key-value pairs
	 */
	@SuppressWarnings("unchecked")
	public java.util.Map<Object, Object> hashGetAllAndDelete(String key)
	{
		try
		{
			String script = "local data = redis.call('HGETALL', KEYS[1])\n" + "if table.getn(data) > 0 then\n"
					+ "    redis.call('DEL', KEYS[1])\n" + "end\n" + "return data";
			org.springframework.data.redis.core.script.RedisScript<List> redisScript = org.springframework.data.redis.core.script.RedisScript
					.of(script, List.class);
			List<Object> result = redisTemplate.execute(redisScript, java.util.Collections.singletonList(key));

			if (result == null || result.isEmpty())
			{
				return java.util.Collections.emptyMap();
			}

			java.util.Map<Object, Object> map = new java.util.HashMap<>();
			for (int i = 0; i < result.size(); i += 2)
			{
				map.put(result.get(i), result.get(i + 1));
			}
			return map;
		}
		catch (Exception e)
		{
			log.error("Error executing atomic hashGetAllAndDelete for key: {}", key, e);
			return java.util.Collections.emptyMap();
		}
	}
}
