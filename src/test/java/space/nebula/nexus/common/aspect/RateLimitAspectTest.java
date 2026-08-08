package space.nebula.nexus.common.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

	@AfterEach
	void clearRequestContext() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void usesUniqueSortedSetMembersForConcurrentRequests() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any())).thenReturn(1L);

		RateLimitAspect aspect = new RateLimitAspect(redisTemplate, true);
		RateLimit rateLimit = rateLimit();
		JoinPoint joinPoint = joinPoint();
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("203.0.113.42");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		aspect.doBefore(joinPoint, rateLimit);

		ArgumentCaptor<String> timestampCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> requestMemberCaptor = ArgumentCaptor.forClass(String.class);
		verify(redisTemplate).execute(any(), eq(List.of(CacheConstants.RATE_LIMIT_PREFIX + "test:203.0.113.42:TestHandler.handle(..)")),
				anyString(), anyString(), timestampCaptor.capture(), requestMemberCaptor.capture());
		assertNotEquals(timestampCaptor.getValue(), requestMemberCaptor.getValue());
		assertTrue(requestMemberCaptor.getValue().startsWith(timestampCaptor.getValue() + ":"));
		assertTrue(RateLimitAspect.RATE_LIMIT_LUA.contains("redis.call('zadd', key, now, request_id)"));
	}

	@Test
	void rejectsRequestsWhenRedisReportsTheLimitIsExceeded() throws Exception {
		StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
		when(redisTemplate.execute(any(), anyList(), any(), any(), any(), any())).thenReturn(0L);

		RateLimitAspect aspect = new RateLimitAspect(redisTemplate, true);
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

		assertThrows(BusinessException.class, () -> aspect.doBefore(joinPoint(), rateLimit()));
	}

	private RateLimit rateLimit() throws Exception {
		return TestHandler.class.getDeclaredMethod("handle").getAnnotation(RateLimit.class);
	}

	private JoinPoint joinPoint() {
		MethodSignature signature = mock(MethodSignature.class);
		when(signature.toShortString()).thenReturn("TestHandler.handle(..)");

		JoinPoint joinPoint = mock(JoinPoint.class);
		when(joinPoint.getSignature()).thenReturn(signature);
		return joinPoint;
	}

	private static class TestHandler {

		@RateLimit(key = "test", count = 1, time = 1, unit = TimeUnit.MINUTES)
		void handle() {
		}
	}
}
