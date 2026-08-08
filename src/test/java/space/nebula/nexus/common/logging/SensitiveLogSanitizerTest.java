package space.nebula.nexus.common.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import space.nebula.nexus.payload.request.ConfigRequest;
import space.nebula.nexus.payload.response.AuthResponse;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SensitiveLogSanitizerTest {

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final SensitiveLogSanitizer sanitizer = new SensitiveLogSanitizer();

	@Test
	void recursivelyMasksSensitiveRequestFields() throws Exception {
		String sanitized = sanitizer.sanitize(Map.of("password", "current-password", "profile",
				Map.of("otpCode", "123456", "displayName", "Nexus"), "webhookSecret", "webhook-secret"));

		JsonNode result = objectMapper.readTree(sanitized);
		assertEquals("******", result.get("password").asText());
		assertEquals("******", result.get("profile").get("otpCode").asText());
		assertEquals("******", result.get("webhookSecret").asText());
		assertEquals("Nexus", result.get("profile").get("displayName").asText());
	}

	@Test
	void masksAConfigurationValueWhenItsKeyIsSensitive() throws Exception {
		ConfigRequest request = new ConfigRequest("jwt.secret", "super-secret", "JWT signing key", null, false);

		JsonNode result = objectMapper.readTree(sanitizer.sanitize(request));
		assertEquals("jwt.secret", result.get("configKey").asText());
		assertEquals("******", result.get("configValue").asText());
	}

	@Test
	void masksTokensWhenAResultIsAudited() throws Exception {
		AuthResponse response = AuthResponse.builder().accessToken("access-token").refreshToken("refresh-token")
				.username("nexus").email("nexus@example.com").roles(Set.of("ROLE_USER")).build();

		JsonNode result = objectMapper.readTree(sanitizer.sanitize(response));
		assertEquals("******", result.get("accessToken").asText());
		assertEquals("******", result.get("refreshToken").asText());
		assertEquals("nexus", result.get("username").asText());
	}

	@Test
	void reSanitizesLegacySerializedAuditData() throws Exception {
		JsonNode result = objectMapper.readTree(sanitizer.sanitizeSerializedJson(
				"{\"request\":{\"newPassword\":\"new-password\",\"nested\":{\"secret\":\"value\"}}}"));

		assertEquals("******", result.get("request").get("newPassword").asText());
		assertEquals("******", result.get("request").get("nested").get("secret").asText());
		assertEquals("[omitted]", sanitizer.sanitizeSerializedJson("not-json"));
	}
}
