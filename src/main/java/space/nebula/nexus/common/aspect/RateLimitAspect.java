package space.nebula.nexus.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.utils.IpUtil;

import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

/**
 * Enterprise-grade Rate Limit Aspect using Redis Lua scripts. Implements a
 * Sliding Window algorithm for precise traffic control.
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

	private final StringRedisTemplate stringRedisTemplate;
	private final boolean enabled;

	public RateLimitAspect(StringRedisTemplate stringRedisTemplate,
			@org.springframework.beans.factory.annotation.Value("${app.security.rate-limit.enabled:true}") boolean enabled) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.enabled = enabled;
	}

	// Lua script for Sliding Window Rate Limiting
	// ARGV[1]: window size in milliseconds
	// ARGV[2]: max requests in window
	// ARGV[3]: current timestamp in milliseconds
	// ARGV[4]: unique request member so concurrent requests in the same millisecond
	// do not overwrite each other in the sorted set.
	static final String RATE_LIMIT_LUA = "local key = KEYS[1] " + "local window = tonumber(ARGV[1]) "
			+ "local limit = tonumber(ARGV[2]) " + "local now = tonumber(ARGV[3]) "
			+ "local request_id = ARGV[4] "
			+ "redis.call('zremrangebyscore', key, 0, now - window) "
			+ "local current_count = redis.call('zcard', key) " + "if current_count < limit then "
			+ "  redis.call('zadd', key, now, request_id) " + "  redis.call('pexpire', key, window) "
			+ "  return 1 " + "else "
			+ "  return 0 " + "end";

	@Before("@annotation(rateLimit)")
	public void doBefore(JoinPoint joinPoint, RateLimit rateLimit) {
		if (!enabled) {
			return;
		}

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();

		// 1. Generate a unique and safe limit key
		String ip = IpUtil.getIpAddress(request);
		String methodName = ((MethodSignature) joinPoint.getSignature()).toShortString();
		String combinedKey = CacheConstants.RATE_LIMIT_PREFIX + rateLimit.key() + ":" + ip + ":" + methodName;

		// 2. Prepare parameters for Lua script
		long windowSizeMillis = rateLimit.unit().toMillis(rateLimit.time());
		long maxRequests = rateLimit.count();
		long nowMillis = System.currentTimeMillis();
		String requestMember = nowMillis + ":" + UUID.randomUUID();

		// 3. Execute Lua script atomically
		DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_LUA, Long.class);
		Long result = stringRedisTemplate.execute(script, Collections.singletonList(combinedKey),
				String.valueOf(windowSizeMillis), String.valueOf(maxRequests), String.valueOf(nowMillis), requestMember);

		// 4. Handle result
		if (result == null || result == 0) {
			log.warn("Rate limit exceeded for IP: {} on method: {}. Key: {}", ip, methodName, combinedKey);
			throw new BusinessException(BusinessCode.TOO_MANY_REQUESTS, rateLimit.message());
		}
	}
}
