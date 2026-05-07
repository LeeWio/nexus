package space.nebula.nexus.common.aspect;

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import space.nebula.nexus.common.annotation.LogOperation;

import java.lang.reflect.Method;

/**
 * Aspect for handling @LogOperation annotation.
 */
@Aspect
@Component
@Slf4j
public class LogOperationAspect {

    @Around("@annotation(logOperation)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        // 1. Get request info
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // 2. Get user info
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "Anonymous";
        
        // 3. Get method info
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            logError(username, logOperation.value(), methodName, startTime, e);
            throw e;
        }
        
        logSuccess(username, logOperation.value(), methodName, startTime, logOperation, joinPoint.getArgs(), result);
        return result;
    }

    private void logSuccess(String username, String description, String methodName, long startTime, 
                            LogOperation annotation, Object[] args, Object result) {
        long duration = System.currentTimeMillis() - startTime;
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- Operation Log [SUCCESS] ---\n");
        sb.append("Operator    : ").append(username).append("\n");
        sb.append("Description : ").append(description).append("\n");
        sb.append("Method      : ").append(methodName).append("\n");
        sb.append("Duration    : ").append(duration).append("ms\n");
        
        if (annotation.logArgs() && args != null && args.length > 0) {
            sb.append("Arguments   : ").append(JSONUtil.toJsonStr(args)).append("\n");
        }
        
        if (annotation.logResult() && result != null) {
            sb.append("Result      : ").append(JSONUtil.toJsonStr(result)).append("\n");
        }
        sb.append("-------------------------------");
        
        log.info(sb.toString());
    }

    private void logError(String username, String description, String methodName, long startTime, Throwable e) {
        long duration = System.currentTimeMillis() - startTime;
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- Operation Log [FAILED] ---\n");
        sb.append("Operator    : ").append(username).append("\n");
        sb.append("Description : ").append(description).append("\n");
        sb.append("Method      : ").append(methodName).append("\n");
        sb.append("Duration    : ").append(duration).append("ms\n");
        sb.append("Error       : ").append(e.getMessage()).append("\n");
        sb.append("------------------------------");
        
        log.error(sb.toString());
    }
}
