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
            log.error("Business error [{}]: {}", code, e.getMessage());
        } else {
            log.warn("Business warning [{}]: {}", code, e.getMessage());
        }
        return ResponseEntity.status(status).body(ApiResponse.error(code, e.getMessage()));
    }

    /**
     * Handle file upload size limit exceeded.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("Upload size limit exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(413, "File too large. Maximum permitted size exceeded."));
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
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, "Validation failed: " + message));
    }

    /**
     * Handle ConstraintViolationException (e.g., @Validated on controller parameters).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("Constraint violation: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, "Parameter validation failed: " + message));
    }

    /**
     * Handle Spring Security Access Denied.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(403, "Access denied: you do not have sufficient permissions"));
    }

    /**
     * Handle Authentication failures.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());
        String message = "Authentication failed";
        if (e instanceof BadCredentialsException) {
            message = "Invalid username or password";
        } else if (e instanceof DisabledException) {
            message = "Your account is currently pending audit or has been disabled. Please contact the administrator.";
        } else if (e instanceof LockedException) {
            message = "Your account is locked due to security reasons.";
        } else {
            message += ": " + e.getMessage();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(401, message));
    }

    /**
     * Handle all other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        log.error("Unexpected system error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "Internal server error: " + e.getMessage()));
    }
}
