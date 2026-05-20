package space.nebula.nexus.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.slf4j.MDC;
import space.nebula.nexus.common.constant.BusinessCode;

import java.io.Serializable;

/**
 * Standard unified API response wrapper.
 * Implemented as a Java 21 Record for immutability and conciseness.
 */
@Builder(toBuilder = true)
@Schema(description = "Standard API Response Wrapper")
public record ApiResponse<T>(
    @Schema(description = "Business response code", example = "200")
    int code,
    
    @Schema(description = "Response message", example = "Operation successful")
    String message,
    
    @Schema(description = "Response data payload")
    T data,
    
    @Schema(description = "Log trace ID for troubleshooting")
    String traceId
) implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a success response with no data.
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * Creates a success response with a data payload.
     */
    public static <T> ApiResponse<T> success(T data) {
        return success("Success", data);
    }

    /**
     * Creates a success response with a custom message and data payload.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, MDC.get("traceId"));
    }

    /**
     * Creates an error response with a specific code and message.
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, MDC.get("traceId"));
    }

    /**
     * Creates an error response from a BusinessCode enum.
     */
    public static <T> ApiResponse<T> error(BusinessCode code) {
        return error(code.getCode(), code.getMessage());
    }

    /**
     * Creates an error response from a BusinessCode enum with a custom message.
     */
    public static <T> ApiResponse<T> error(BusinessCode code, String message) {
        return error(code.getCode(), message);
    }
}
