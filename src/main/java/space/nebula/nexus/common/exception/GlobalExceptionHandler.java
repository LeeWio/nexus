package space.nebula.nexus.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;

import java.util.stream.Collectors;

/**
 * Global exception handler and data binder to unify error responses and parameter handling.
 * Refined for RESTful standards: returning appropriate HTTP status codes.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Globally trim all strings in request parameters and body.
     */
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        StringTrimmerEditor stringTrimmerEditor = new StringTrimmerEditor(true);
        binder.registerCustomEditor(String.class, stringTrimmerEditor);
    }

    /**
     * Handle custom business exceptions.
     * Maps the internal business code to HTTP status if they are standard (4xx, 500).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        int code = e.getCode();
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) status = HttpStatus.BAD_REQUEST;

        if (code >= 500) {
            log.error("[TraceId: {}] Business error [{}]: {}", org.slf4j.MDC.get("traceId"), code, e.getMessage());
        } else {
            log.warn("[TraceId: {}] Business warning [{}]: {}", org.slf4j.MDC.get("traceId"), code, e.getMessage());
        }
        return ResponseEntity.status(status).body(ApiResponse.error(code, e.getMessage()));
    }

    /**
     * Handle file upload size limit exceeded.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[TraceId: {}] Upload size limit exceeded: {}", org.slf4j.MDC.get("traceId"), e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(BusinessCode.FILE_TOO_LARGE.getCode(), BusinessCode.FILE_TOO_LARGE.getMessage()));
    }

    /**
     * Handle validation exceptions for @Valid on RequestBody.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception e) {
        String message;
        if (e instanceof MethodArgumentNotValidException me) {
            message = me.getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else {
            message = ((BindException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        }
        log.warn("[TraceId: {}] Validation failed: {}", org.slf4j.MDC.get("traceId"), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(BusinessCode.VALIDATION_FAILED.getCode(), "Validation failed: " + message));
    }

    /**
     * Handle ConstraintViolationException (e.g., @Validated on controller parameters).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("[TraceId: {}] Constraint violation: {}", org.slf4j.MDC.get("traceId"), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(BusinessCode.BAD_REQUEST.getCode(), "Parameter validation failed: " + message));
    }

    /**
     * Handle Spring Security Access Denied.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[TraceId: {}] Access denied: {}", org.slf4j.MDC.get("traceId"), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(BusinessCode.FORBIDDEN.getCode(), BusinessCode.FORBIDDEN.getMessage()));
    }

    /**
     * Handle Authentication failures.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("[TraceId: {}] Authentication failed: {}", org.slf4j.MDC.get("traceId"), e.getMessage());
        
        BusinessCode code = BusinessCode.UNAUTHORIZED;
        String message = code.getMessage();
        
        if (e instanceof BadCredentialsException) {
            code = BusinessCode.BAD_CREDENTIALS;
            message = code.getMessage();
        } else if (e instanceof DisabledException) {
            code = BusinessCode.ACCOUNT_DISABLED;
            message = code.getMessage();
        } else if (e instanceof LockedException) {
            code = BusinessCode.ACCOUNT_LOCKED;
            message = code.getMessage();
        }
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(code.getCode(), message));
    }

    /**
     * Handle all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        log.error("[TraceId: {}] Unexpected system error", org.slf4j.MDC.get("traceId"), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(BusinessCode.ERROR.getCode(), BusinessCode.ERROR.getMessage() + ": " + e.getMessage()));
    }
}
