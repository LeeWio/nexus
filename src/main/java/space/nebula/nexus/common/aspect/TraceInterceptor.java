package space.nebula.nexus.common.aspect;

import cn.hutool.core.util.IdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to inject a unique Trace ID into MDC for log correlation.
 */
@Component
public class TraceInterceptor implements HandlerInterceptor {

	public static final String TRACE_ID_KEY = "traceId";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		String traceId = request.getHeader("X-Trace-Id");
		if (traceId == null || traceId.isBlank()) {
			traceId = IdUtil.fastSimpleUUID();
		}
		MDC.put(TRACE_ID_KEY, traceId);
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		MDC.remove(TRACE_ID_KEY);
	}
}
