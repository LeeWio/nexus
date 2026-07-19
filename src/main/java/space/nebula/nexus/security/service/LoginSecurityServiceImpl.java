package space.nebula.nexus.security.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.utils.RedisUtil;

import java.util.concurrent.TimeUnit;

/**
 * Implementation of LoginSecurityService using Redis for state management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityServiceImpl implements LoginSecurityService
{

	private final RedisUtil redisUtil;
	private final space.nebula.nexus.config.AuthProperties authProperties;

	@Override
	public void validateLoginLock(String username)
	{
		String failKey = CacheConstants.LOGIN_FAIL_COUNT + username;
		Integer fails = redisUtil.get(failKey, Integer.class).orElse(0);

		if (fails >= authProperties.getMaxLoginFailures())
		{
			Long expire = redisUtil.getExpire(failKey);
			long minutesLeft = (expire != null && expire > 0) ? (expire / 60 + 1) : authProperties.getLockDurationMinutes();

			log.warn("Login attempt blocked for locked user: {}", username);
			throw new BusinessException(403, StrUtil.format(
					"Account is locked. Try again in {} minutes.", minutesLeft));
		}
	}

	@Override
	public void recordLoginFailure(String username)
	{
		String failKey = CacheConstants.LOGIN_FAIL_COUNT + username;
		Long count = redisUtil.increment(failKey, 1);

		if (count != null && count == 1)
		{
			redisUtil.expire(failKey, authProperties.getLockDurationMinutes(), TimeUnit.MINUTES);
		}

		log.warn("Login failure recorded for user: {}. Failure count: {}", username, count);

		if (count != null && count >= authProperties.getMaxLoginFailures())
		{
			log.error("User {} has been locked for {} minutes after {} failed attempts", username,
					authProperties.getLockDurationMinutes(), count);
		}
	}

	@Override
	public void resetLoginFailure(String username)
	{
		String failKey = CacheConstants.LOGIN_FAIL_COUNT + username;
		redisUtil.delete(failKey);
		log.debug("Login failure count reset for user: {}", username);
	}
}
