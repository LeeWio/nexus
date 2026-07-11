package space.nebula.nexus.security.token;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/** Redis implementation of the revoked-token store. */
@Component
@RequiredArgsConstructor
public class RedisRevokedTokenStore implements RevokedTokenStore {

	private static final String KEY_PREFIX = "nexus:jwt:blacklist:";
	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	public void revoke(String token, Duration timeToLive) {
		redisTemplate.opsForValue().set(key(token), "revoked", timeToLive);
	}

	@Override
	public boolean isRevoked(String token) {
		return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
	}

	private String key(String token) {
		return KEY_PREFIX + token;
	}
}
