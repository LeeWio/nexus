package space.nebula.nexus.security.service;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
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
public class LoginSecurityServiceImpl implements LoginSecurityService
{

	private static final int MAX_LOGIN_FAILURES = 5;
	private static final long LOCK_DURATION_MINUTES = 15;

	@Resource
	private RedisUtil redisUtil;

	@Override
	public void validateLoginLock(String username)
	{
		String failKey = CacheConstants.LOGIN_FAIL_COUNT + username;
		Integer fails = redisUtil.get(failKey, Integer.class).orElse(0);

		if (fails >= MAX_LOGIN_FAILURES)
		{
			Long expire = redisUtil.getExpire(failKey);
			long minutesLeft = (expire != null && expire > 0) ? (expire / 60 + 1) : LOCK_DURATION_MINUTES;

			log.warn("Login attempt blocked for locked user: {}", username);
			throw new BusinessException(403, StrUtil.format(
					"Account is locked due to too many failed attempts. Please try again in {} minutes.", minutesLeft));
		}
	}

	@Override
	public void recordLoginFailure(String username)
	{
		String failKey = CacheConstants.LOGIN_FAIL_COUNT + username;
		Long count = redisUtil.increment(failKey, 1);

		if (count != null && count == 1)
		{
			redisUtil.expire(failKey, LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
		}

		log.warn("Login failure recorded for user: {}. Failure count: {}", username, count);

		if (count != null && count >= MAX_LOGIN_FAILURES)
		{
			log.error("User {} has been locked for {} minutes after {} failed attempts", username,
					LOCK_DURATION_MINUTES, count);
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
