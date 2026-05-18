package space.nebula.nexus.common.aspect;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import space.nebula.nexus.entity.VisitLog;
import space.nebula.nexus.utils.IpUtil;

import java.time.LocalDateTime;

/**
 * Interceptor to capture analytics data for public requests.
 * Buffers logs in Redis to ensure low latency for users.
 */
@Slf4j
@Component
public class AnalyticsInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ANALYTICS_BUFFER_KEY = "nexus:analytics:buffer";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            String url = request.getRequestURL().toString();
            // Only track public API requests, excluding static assets or documentation
            if (url.contains("/api/v1/public/") && !url.contains("/seo/")) {
                VisitLog visitLog = new VisitLog();
                visitLog.setIpAddress(IpUtil.getIpAddress(request));
                visitLog.setUserAgent(request.getHeader("User-Agent"));
                visitLog.setReferer(request.getHeader("Referer"));
                visitLog.setRequestUrl(request.getRequestURI());
                visitLog.setVisitTime(LocalDateTime.now());
                
                // Note: Browser/OS/Location will be parsed in the background task to save time here
                
                redisTemplate.opsForList().rightPush(ANALYTICS_BUFFER_KEY, visitLog);
            }
        } catch (Exception e) {
            log.error("Failed to buffer analytics log", e);
        }
        return true;
    }
}
