package space.nebula.nexus.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import space.nebula.nexus.config.AnalyticsProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AnalyticsPrivacyServiceTest {

	@Test
	void hashesVisitorAndReusesAnonymousSessionCookie() {
		AnalyticsProperties properties = new AnalyticsProperties();
		properties.setHashSalt("test-analytics-pepper");
		AnalyticsPrivacyService service = new AnalyticsPrivacyService(properties);
		MockHttpServletRequest firstRequest = new MockHttpServletRequest();
		firstRequest.setRemoteAddr("203.0.113.8");
		MockHttpServletResponse firstResponse = new MockHttpServletResponse();

		String visitorHash = service.hashVisitor(firstRequest);
		String sessionId = service.resolveSessionId(firstRequest, firstResponse);

		assertNotNull(sessionId);
		assertEquals(64, visitorHash.length());
		assertFalse(visitorHash.contains("203.0.113.8"));
		assertNotNull(firstResponse.getHeader("Set-Cookie"));

		MockHttpServletRequest followUpRequest = new MockHttpServletRequest();
		followUpRequest.setCookies(new jakarta.servlet.http.Cookie(AnalyticsPrivacyService.SESSION_COOKIE, sessionId));
		assertEquals(sessionId, service.resolveSessionId(followUpRequest, new MockHttpServletResponse()));
	}
}
