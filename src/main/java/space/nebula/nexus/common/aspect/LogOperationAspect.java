package space.nebula.nexus.common.aspect;

import cn.hutool.core.util.ObjectUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.logging.SensitiveLogSanitizer;
import space.nebula.nexus.entity.OperationLog;
import space.nebula.nexus.utils.IpUtil;
import space.nebula.nexus.utils.RedisUtil;

import java.lang.reflect.Method;

/**
 * Enhanced Aspect for handling @LogOperation. Captures detailed metadata and
 * buffers to Redis for async persistence.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LogOperationAspect {

	private final RedisUtil redisUtil;

	private final SensitiveLogSanitizer sensitiveLogSanitizer;

	@Around("@annotation(logOperation)")
	public Object around(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {
		long startTime = System.currentTimeMillis();

		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest request = ObjectUtil.isNotNull(attributes) ? attributes.getRequest() : null;

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String username = ObjectUtil.isNotNull(authentication) ? authentication.getName() : "Anonymous";

		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();

		Object result = null;
		Throwable exception = null;
		int status = 1; // Success

		try {
			result = joinPoint.proceed();
		} catch (Throwable e) {
			exception = e;
			status = 0; // Failure
			throw e;
		} finally {
			try {
				saveLog(username, logOperation, methodName, startTime, request, joinPoint, result, exception, status);
			} catch (Exception e) {
				log.error("Failed to buffer operation log", e);
			}
		}

		return result;
	}

	private void saveLog(String username, LogOperation annotation, String methodName, long startTime,
			HttpServletRequest request, ProceedingJoinPoint joinPoint, Object result, Throwable exception, int status) {
		long duration = System.currentTimeMillis() - startTime;

		OperationLog opLog = new OperationLog();
		opLog.setUsername(username);
		opLog.setDescription(annotation.value());
		opLog.setMethodName(methodName);
		opLog.setDuration(duration);
		opLog.setStatus(status);
		opLog.setTraceId(MDC.get("traceId"));

		if (ObjectUtil.isNotNull(request)) {
			opLog.setRequestMethod(request.getMethod());
			opLog.setRequestUrl(request.getRequestURI());
			opLog.setIpAddress(IpUtil.getIpAddress(request));
			opLog.setUserAgent(request.getHeader("User-Agent"));
		}

		if (annotation.logArgs() && joinPoint.getArgs() != null && joinPoint.getArgs().length > 0) {
			opLog.setParameters(getSafeArgsJson((MethodSignature) joinPoint.getSignature(), joinPoint.getArgs()));
		}

		if (annotation.logResult() && ObjectUtil.isNotNull(result)) {
			opLog.setResult(sensitiveLogSanitizer.sanitize(result));
		}

		if (ObjectUtil.isNotNull(exception)) {
			opLog.setErrorMessage(exception.getMessage());
		}

		// Buffer to Redis list for async flush
		redisUtil.listAdd(CacheConstants.OPERATION_LOG_BUFFER_KEY, opLog);
		log.debug("Operation buffered: {} by {}", annotation.value(), username);
	}

	private String getSafeArgsJson(MethodSignature signature, Object[] args) {
		return sensitiveLogSanitizer.sanitizeArguments(signature, args);
	}
}
