package space.nebula.nexus.security.token;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.utils.RedisUtil;

/**
 * Redis-backed one-time OAuth login code store using {@link RedisUtil}.
 * Payloads are stored as JSON strings for stable round-trips through RedisUtil.
 */
@Component
@RequiredArgsConstructor
public class RedisOAuthLoginCodeStore implements OAuthLoginCodeStore {

	static final String KEY_PREFIX = "nexus:oauth:login-code:";
	static final long TTL_SECONDS = 60L;

	private final RedisUtil redisUtil;
	private final ObjectMapper objectMapper;

	@Override
	public String issue(AuthResponse authResponse) {
		String code = UUID.randomUUID().toString().replace("-", "");
		try {
			String payload = objectMapper.writeValueAsString(authResponse);
			boolean stored = redisUtil.set(KEY_PREFIX + code, payload, TTL_SECONDS, TimeUnit.SECONDS);
			if (!stored) {
				throw new IllegalStateException("Failed to persist OAuth login code");
			}
			return code;
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Failed to serialize OAuth login payload", e);
		}
	}

	@Override
	public Optional<AuthResponse> consume(String code) {
		if (code == null || code.isBlank()) {
			return Optional.empty();
		}
		return redisUtil.getAndDelete(KEY_PREFIX + code.trim(), String.class).flatMap(payload -> {
			try {
				return Optional.of(objectMapper.readValue(payload, AuthResponse.class));
			} catch (JsonProcessingException e) {
				return Optional.empty();
			}
		});
	}
}
