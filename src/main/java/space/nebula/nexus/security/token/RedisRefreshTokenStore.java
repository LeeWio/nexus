package space.nebula.nexus.security.token;

import java.time.Duration;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/** Redis-backed refresh-token store using atomic consume semantics. */
@Component
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

	private static final String KEY_PREFIX = "nexus:jwt:refresh:";
	private static final RedisScript<Long> CONSUME_SCRIPT = RedisScript.of(
			"if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end",
			Long.class);
	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	public void issue(String tokenId, String username, Duration timeToLive) {
		redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, username, timeToLive);
	}

	@Override
	public boolean consume(String tokenId, String username) {
		Long result = redisTemplate.execute(CONSUME_SCRIPT, List.of(KEY_PREFIX + tokenId), username);
		return result != null && result == 1L;
	}
}
