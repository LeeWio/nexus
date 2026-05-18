package space.nebula.nexus.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor for adding a unique Trace ID to every request.
 * Helps in correlating logs for a single transaction.
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    private static final String TRACE_ID = "traceId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate or retrieve trace ID
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        
        // Put in MDC for logging
        MDC.put(TRACE_ID, traceId);
        
        // Set in response header for client-side tracking
        response.setHeader("X-Trace-Id", traceId);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // Clear MDC to prevent memory leaks in thread pools
        MDC.remove(TRACE_ID);
    }
}
