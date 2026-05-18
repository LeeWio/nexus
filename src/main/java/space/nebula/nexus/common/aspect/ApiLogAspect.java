package space.nebula.nexus.common.aspect;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import space.nebula.nexus.utils.IpUtil;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Global Aspect for logging all API requests and responses.
 * Provides performance monitoring and audit tracking.
 */
@Aspect
@Component
@Slf4j
public class ApiLogAspect {

    /**
     * Pointcut that matches all methods in any class under 'controller' package.
     */
    @Pointcut("execution(* space.nebula.nexus.controller..*.*(..))")
    public void apiMethods() {}

    @Around("apiMethods()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String url = request.getRequestURL().toString();
        String method = request.getMethod();
        String ip = IpUtil.getIpAddress(request);
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // Log request
        log.info(">>> API Request: [{} {}] from IP: {} | Controller: {}.{}", 
                method, url, ip, className, methodName);
        
        // Log parameters (excluding potentially sensitive ones)
        String params = getSafeParamsJson(joinPoint);
        if (!params.equals("{}")) {
            log.debug(">>> API Parameters: {}", params);
        }

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("<<< API Failed: [{} {}] | Duration: {}ms | Error: {}", 
                    method, url, duration, e.getMessage());
            throw e;
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("<<< API Response: [{} {}] | Duration: {}ms", method, url, duration);
        
        return result;
    }

    /**
     * Serializes method arguments while masking sensitive fields like 'password'.
     */
    private String getSafeParamsJson(ProceedingJoinPoint joinPoint) {
        try {
            String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
            Object[] args = joinPoint.getArgs();

            if (parameterNames == null || args == null) return "{}";

            Map<String, Object> paramsMap = IntStream.range(0, parameterNames.length)
                .boxed()
                .collect(Collectors.toMap(
                    i -> parameterNames[i],
                    i -> {
                        Object arg = args[i];
                        if (arg == null) return "null";
                        
                        String name = parameterNames[i].toLowerCase();
                        // Simple masking for common sensitive fields
                        if (name.contains("password") || name.contains("secret") || name.contains("token")) {
                            return "******";
                        }
                        return arg;
                    },
                    (v1, v2) -> v1
                ));

            return JSONUtil.toJsonStr(paramsMap);
        } catch (Exception e) {
            return "{ \"error\": \"Failed to serialize parameters\" }";
        }
    }
}
