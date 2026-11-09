package space.nebula.nexus.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import space.nebula.nexus.service.AnalyticsPrivacyService;

/**
 * Interceptor to capture analytics data for public requests. Buffers logs in
 * Redis to ensure low latency for users.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsInterceptor implements HandlerInterceptor {

	@SuppressWarnings("unused")
	private final RedisTemplate<String, Object> redisTemplate;
	@SuppressWarnings("unused")
	private final AnalyticsPrivacyService analyticsPrivacyService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		// Page views are recorded by POST /api/v1/public/analytics/pageviews.
		// Counting every public GET treated polls and homepage widgets as visits.
		return true;
	}

	/**
	 * Prefer the browser agent forwarded by the Next.js rewrite. The servlet
	 * User-Agent is the Node proxy, which would classify every visitor as desktop.
	 */
	static boolean isServerPrefetch(HttpServletRequest request) {
		return "1".equals(request.getHeader("X-Odyssey-Prefetch"));
	}

	static String clientUserAgent(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Odyssey-Client-UA");
		if (forwarded != null && !forwarded.isBlank() && forwarded.length() <= 500) {
			return forwarded;
		}
		return request.getHeader("User-Agent");
	}
}
