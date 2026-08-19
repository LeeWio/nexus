package space.nebula.nexus.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import space.nebula.nexus.config.AnalyticsProperties;
import space.nebula.nexus.utils.IpUtil;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

/** Resolves anonymous analytics identities without persisting raw IP addresses. */
@Service
@RequiredArgsConstructor
public class AnalyticsPrivacyService {
	public static final String SESSION_COOKIE = "NEXUS_ANALYTICS_SESSION";
	private static final Pattern SESSION_ID = Pattern.compile("^[0-9a-fA-F-]{36}$");

	private final AnalyticsProperties properties;

	public String hashVisitor(HttpServletRequest request) {
		return hmac(IpUtil.getIpAddress(request));
	}

	public String resolveSessionId(HttpServletRequest request, HttpServletResponse response) {
		String supplied = request.getHeader("X-Analytics-Session");
		if (isValidSessionId(supplied)) {
			return supplied;
		}

		if (request.getCookies() != null) {
			for (Cookie cookie : request.getCookies()) {
				if (SESSION_COOKIE.equals(cookie.getName()) && isValidSessionId(cookie.getValue())) {
					return cookie.getValue();
				}
			}
		}

		String generated = UUID.randomUUID().toString();
		ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, generated).path("/")
				.maxAge(Duration.ofDays(properties.getSessionCookieMaxAgeDays())).sameSite("Lax")
				.secure(request.isSecure()).build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return generated;
	}

	private boolean isValidSessionId(String value) {
		return value != null && SESSION_ID.matcher(value).matches();
	}

	private String hmac(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(properties.getHashSalt().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to generate anonymous analytics identifier", exception);
		}
	}
}
