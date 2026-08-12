package space.nebula.nexus.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.utils.MessageUtil;
import lombok.RequiredArgsConstructor;
import cn.hutool.core.util.StrUtil;

import java.util.stream.Collectors;

/**
 * Global exception handler and data binder to unify error responses and
 * parameter handling. Adheres to RESTful standards by returning appropriate
 * HTTP status codes and structured payloads.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final MessageUtil messageUtil;
	private final Environment environment;

	/**
	 * Globally trim all strings in request parameters and body to prevent
	 * whitespace pollution.
	 */
	@InitBinder
	public void initBinder(WebDataBinder binder) {
		var stringTrimmerEditor = new StringTrimmerEditor(true);
		binder.registerCustomEditor(String.class, stringTrimmerEditor);
	}

	/**
	 * Handle custom business exceptions.
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
		var code = e.getCode();
		var status = HttpStatus.resolve(code);
		if (status == null)
			status = HttpStatus.BAD_REQUEST;

		String message = e.getMessage();

		// Determine if the message in the exception is the generic default enum
		// message.
		// If it is custom (e.g. hand-written details), we keep it and do not overwrite
		// it.
		boolean isDefaultMessage = false;
		for (BusinessCode bc : BusinessCode.values()) {
			if (bc.getCode() == code && StrUtil.equals(bc.getMessage(), message)) {
				isDefaultMessage = true;
				break;
			}
		}

		if (isDefaultMessage || StrUtil.isBlank(message) || e.getArgs() != null) {
			try {
				// Resolve the standard English message template for the business code.
				String localizedMessage = messageUtil.get(String.valueOf(code), e.getArgs());
				if (StrUtil.isNotBlank(localizedMessage)) {
					message = localizedMessage;
				}
			} catch (Exception ex) {
				// Fallback to original message
			}
		}

		var traceId = org.slf4j.MDC.get("traceId");
		if (code >= 500) {
			log.error("[TraceId: {}] Business error [{}]: {}", traceId, code, message);
		} else {
			log.warn("[TraceId: {}] Business warning [{}]: {}", traceId, code, message);
		}
		return ResponseEntity.status(status).body(ApiResponse.error(code, message));
	}

	/**
	 * Handle file upload size limit exceeded.
	 */
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
		log.warn("[TraceId: {}] Upload size limit exceeded: {}", org.slf4j.MDC.get("traceId"), e.getMessage());
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.error(BusinessCode.FILE_TOO_LARGE));
	}

	/**
	 * Handle validation exceptions for @Valid annotated payloads.
	 */
	@ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
	public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception e) {
		String message;
		if (e instanceof MethodArgumentNotValidException me) {
			message = me.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage)
					.collect(Collectors.joining(", "));
		} else {
			message = ((BindException) e).getBindingResult().getFieldErrors().stream()
					.map(FieldError::getDefaultMessage).collect(Collectors.joining(", "));
		}
		log.warn("[TraceId: {}] Validation failed: {}", org.slf4j.MDC.get("traceId"), message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(BusinessCode.VALIDATION_FAILED, "Validation failed: " + message));
	}

	/**
	 * Handle missing or malformed JSON request bodies.
	 */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
		log.warn("[TraceId: {}] Request body could not be read: {}", org.slf4j.MDC.get("traceId"), e.getMessage());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(BusinessCode.BAD_REQUEST, "Request body is missing or malformed"));
	}

	/**
	 * Handle ConstraintViolationException (e.g., @Validated on controller
	 * parameters).
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(ConstraintViolationException e) {
		var message = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
				.collect(Collectors.joining(", "));
		log.warn("[TraceId: {}] Constraint violation: {}", org.slf4j.MDC.get("traceId"), message);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiResponse.error(BusinessCode.BAD_REQUEST, "Validation failed: " + message));
	}

	/**
	 * Handle Spring Security Access Denied.
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
		log.warn("[TraceId: {}] Access denied: {}", org.slf4j.MDC.get("traceId"), e.getMessage());
		
		var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || 
				authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken ||
				!authentication.isAuthenticated()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(ApiResponse.error(BusinessCode.UNAUTHORIZED));
		}
		
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(BusinessCode.FORBIDDEN));
	}

	/**
	 * Handle Authentication failures.
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
		log.warn("[TraceId: {}] Authentication failed: {}", org.slf4j.MDC.get("traceId"), e.getMessage());

		BusinessCode businessCode = switch (e) {
			case BadCredentialsException ex -> BusinessCode.BAD_CREDENTIALS;
			case DisabledException ex -> BusinessCode.ACCOUNT_DISABLED;
			case LockedException ex -> BusinessCode.ACCOUNT_LOCKED;
			default -> BusinessCode.UNAUTHORIZED;
		};

		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(businessCode));
	}

	/**
	 * Handle JPA OptimisticLockingFailureException (e.g., @Version conflict).
	 */
	@ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse<Void>> handleOptimisticLockingFailureException(
			org.springframework.dao.OptimisticLockingFailureException e) {
		log.warn("[TraceId: {}] Optimistic lock conflict: {}", org.slf4j.MDC.get("traceId"), e.getMessage());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(
				ApiResponse.error(BusinessCode.BAD_REQUEST.getCode(), "Resource has changed. Refresh and try again."));
	}

	/**
	 * Handle standard Spring MVC exceptions (e.g., 404 No Handler, 405 Method Not
	 * Allowed).
	 */
	@ExceptionHandler(jakarta.servlet.ServletException.class)
	public ResponseEntity<ApiResponse<Void>> handleServletException(jakarta.servlet.ServletException e) {
		log.warn("[TraceId: {}] HTTP Request error: {}", org.slf4j.MDC.get("traceId"), e.getMessage());

		HttpStatus status = switch (e) {
			case org.springframework.web.HttpRequestMethodNotSupportedException ex -> HttpStatus.METHOD_NOT_ALLOWED;
			case org.springframework.web.servlet.NoHandlerFoundException ex -> HttpStatus.NOT_FOUND;
			case org.springframework.web.HttpMediaTypeNotSupportedException ex -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
			default -> HttpStatus.BAD_REQUEST;
		};

		String message = switch (status) {
			case METHOD_NOT_ALLOWED -> "HTTP method is not allowed";
			case NOT_FOUND -> "Requested endpoint was not found";
			case UNSUPPORTED_MEDIA_TYPE -> "Unsupported request content type";
			default -> "Malformed request";
		};
		return ResponseEntity.status(status).body(ApiResponse.error(status.value(), message));
	}

	/**
	 * Handle all other unexpected system exceptions.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
		String traceId = org.slf4j.MDC.get("traceId");
		log.error("[TraceId: {}] Unexpected system error", traceId, e);

		String message = "Unexpected server error";
		if (environment.acceptsProfiles(Profiles.of("prod"))) {
			message = "Unexpected server error. Reference ID: " + traceId;
		}

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiResponse.error(BusinessCode.ERROR, message));
	}
}
