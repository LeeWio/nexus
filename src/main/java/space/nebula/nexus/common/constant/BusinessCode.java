package space.nebula.nexus.common.constant;

import lombok.Getter;

/**
 * Enumeration for business error codes to ensure consistency across the system.
 */
@Getter
public enum BusinessCode {
	// Standard Success/Error
	SUCCESS(200, "Operation successful"), ERROR(500, "Internal server error"),

	// Auth & Security (401xx)
	UNAUTHORIZED(401, "Authentication required"), INVALID_TOKEN(40101, "Token is invalid or expired"), BAD_CREDENTIALS(
			40102, "Invalid username or password"), ACCOUNT_LOCKED(40103, "Account is locked"), ACCOUNT_DISABLED(40104,
					"Account is disabled"), FORBIDDEN(403, "Access denied"),

	// Resource Errors (404xx)
	NOT_FOUND(404, "Resource not found"), USER_NOT_FOUND(40401, "User not found"), POST_NOT_FOUND(40402,
			"Post not found"), CATEGORY_NOT_FOUND(40403, "Category not found"),

	// Request & Validation (400xx)
	BAD_REQUEST(400, "Invalid request"), VALIDATION_FAILED(40001, "Validation failed"), DUPLICATE_KEY(40002,
			"Resource already exists"), FILE_TOO_LARGE(413, "File exceeds the maximum size"), FILE_SIZE_LIMIT(41301,
					"File exceeds the maximum allowed size"), FILE_TYPE_NOT_SUPPORTED(40003, "Unsupported file type"),

	// Infrastructure / Integration
	MAIL_SEND_FAILED(503, "Email delivery failed"),

	// Domain Specific: Posts
	POST_ALREADY_PUBLISHED(40010, "Post is already published"), POST_NOT_PUBLISHED(40011, "Post is not published"),

	// Rate Limiting
	TOO_MANY_REQUESTS(429, "Too many requests");

	private final int code;
	private final String message;

	BusinessCode(int code, String message) {
		this.code = code;
		this.message = message;
	}
}
