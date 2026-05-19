package space.nebula.nexus.common.exception;

import lombok.Getter;
import space.nebula.nexus.common.constant.BusinessCode;

/**
 * Custom exception for business logic errors.
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(BusinessCode businessCode) {
        super(businessCode.getMessage());
        this.code = businessCode.getCode();
    }

    public BusinessException(BusinessCode businessCode, String customMessage) {
        super(customMessage);
        this.code = businessCode.getCode();
    }
}
