package space.nebula.nexus.common.aspect;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.utils.RedisUtil;

import java.util.Objects;

/**
 * Aspect for handling @RateLimit annotation logic.
 */
@Slf4j
@Aspect
@Component
public class RateLimitAspect {

    @Resource
    private RedisUtil redisUtil;

    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint joinPoint, RateLimit rateLimit) {
        String key = rateLimit.key();
        int count = rateLimit.count();
        long time = rateLimit.time();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();

        // Combine key with IP to limit per-client
        String combinedKey = key + getIpAddress(request) + ":" + joinPoint.getSignature().toShortString();

        Long currentCount = redisUtil.increment(combinedKey, 1);
        if (currentCount != null && currentCount == 1) {
            redisUtil.expire(combinedKey, time, rateLimit.unit());
        }

        if (currentCount != null && currentCount > count) {
            log.warn("Rate limit exceeded for key: {}, IP: {}", combinedKey, getIpAddress(request));
            throw new BusinessException(429, rateLimit.message());
        }
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
