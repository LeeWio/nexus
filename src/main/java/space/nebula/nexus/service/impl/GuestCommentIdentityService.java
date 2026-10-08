package space.nebula.nexus.service.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Issues and verifies the cookie that lets a browser claim the guest comments
 * it posted. Only the SHA-256 hash of the token is stored.
 */
@Service
public class GuestCommentIdentityService {

	public static final String COOKIE_NAME = "NEXUS_COMMENT_GUEST";
	private static final Pattern TOKEN = Pattern.compile("^[0-9a-fA-F-]{36}$");
	private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
	private static final Set<String> RESERVED_NAMES = Set.of("admin", "administrator", "moderator", "system",
			"anonymous", "guest", "root", "null", "undefined");
	private static final Duration COOKIE_MAX_AGE = Duration.ofDays(180);

	public Optional<String> currentTokenHash() {
		return readToken().map(this::hashToken);
	}

	/**
	 * Returns the hash that should be stored on a new guest comment, creating the
	 * claim cookie when this browser does not have one yet.
	 */
	public String claimTokenHash(HttpServletRequest request) {
		String token = readToken().orElseGet(() -> issueToken(request));
		return hashToken(token);
	}

	public void validateGuestName(String guestName) {
		if (guestName == null || guestName.isBlank()) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "A guest name is required");
		}
		String normalized = guestName.trim();
		if (normalized.length() > 32) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "Guest name must not exceed 32 characters");
		}
		boolean looksLikeLink = normalized.contains("://") || normalized.toLowerCase().contains("www.");
		boolean looksLikeEmail = normalized.contains("@");
		if (looksLikeLink || looksLikeEmail) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "Guest name cannot contain a link or email address");
		}
		if (RESERVED_NAMES.contains(normalized.toLowerCase())) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "This guest name is reserved");
		}
	}

	public String normalizeGuestEmail(String guestEmail) {
		if (guestEmail == null || guestEmail.isBlank()) {
			return null;
		}
		String normalized = guestEmail.trim();
		if (normalized.length() > 120 || !EMAIL.matcher(normalized).matches()) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "Guest email must be a valid email address");
		}
		return normalized;
	}

	public String hashToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 digest is not available", ex);
		}
	}

	private Optional<String> readToken() {
		ServletRequestAttributes attributes = currentAttributes();
		if (attributes == null) {
			return Optional.empty();
		}
		Cookie[] cookies = attributes.getRequest().getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		for (Cookie cookie : cookies) {
			if (COOKIE_NAME.equals(cookie.getName()) && TOKEN.matcher(cookie.getValue()).matches()) {
				return Optional.of(cookie.getValue());
			}
		}
		return Optional.empty();
	}

	private String issueToken(HttpServletRequest request) {
		String token = UUID.randomUUID().toString();
		ServletRequestAttributes attributes = currentAttributes();
		if (attributes != null) {
			HttpServletResponse response = attributes.getResponse();
			if (response != null) {
				ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token).path("/").httpOnly(true)
						.maxAge(COOKIE_MAX_AGE).sameSite("Lax").secure(request.isSecure()).build();
				response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
			}
		}
		return token;
	}

	private ServletRequestAttributes currentAttributes() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return attributes;
		}
		return null;
	}
}
