package space.nebula.nexus.integration.x;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import space.nebula.nexus.config.XProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin X API client for site-owner posting (OAuth 1.0a user context).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XClient {

	private static final String MEDIA_UPLOAD_URL = "https://upload.twitter.com/1.1/media/upload.json";
	private static final String CREATE_TWEET_URL = "https://api.x.com/2/tweets";

	private final RestClient restClient;
	private final XProperties properties;
	private final ObjectMapper objectMapper;

	public String uploadImage(byte[] bytes, String contentType, String filename) {
		ensureConfigured();
		OAuth1Signer signer = signer();
		String authorization = signer.authorizationHeader("POST", MEDIA_UPLOAD_URL, Map.of());

		MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
		bodyBuilder.part("media", new ByteArrayResource(bytes) {
			@Override
			public String getFilename() {
				return filename == null || filename.isBlank() ? "image.jpg" : filename;
			}
		}).contentType(MediaType.parseMediaType(
				contentType == null || contentType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType));

		try {
			String responseBody = restClient.post().uri(MEDIA_UPLOAD_URL).header("Authorization", authorization)
					.contentType(MediaType.MULTIPART_FORM_DATA).body(bodyBuilder.build()).retrieve().body(String.class);
			JsonNode root = objectMapper.readTree(responseBody);
			String mediaId = root.path("media_id_string").asText(null);
			if (mediaId == null || mediaId.isBlank()) {
				throw new IllegalStateException("X media upload response missing media_id_string");
			}
			return mediaId;
		} catch (RestClientResponseException e) {
			throw new IllegalStateException("X media upload failed: HTTP " + e.getStatusCode().value() + " "
					+ truncate(e.getResponseBodyAsString()), e);
		} catch (Exception e) {
			throw new IllegalStateException("X media upload failed: " + e.getMessage(), e);
		}
	}

	public CreatedPost createPost(String text, List<String> mediaIds) {
		ensureConfigured();
		OAuth1Signer signer = signer();
		String authorization = signer.authorizationHeader("POST", CREATE_TWEET_URL, Map.of());

		Map<String, Object> payload = new LinkedHashMap<>();
		if (text != null && !text.isBlank()) {
			payload.put("text", text);
		}
		if (mediaIds != null && !mediaIds.isEmpty()) {
			payload.put("media", Map.of("media_ids", mediaIds));
		}

		try {
			String responseBody = restClient.post().uri(CREATE_TWEET_URL).header("Authorization", authorization)
					.contentType(MediaType.APPLICATION_JSON).body(payload).retrieve().body(String.class);
			JsonNode data = objectMapper.readTree(responseBody).path("data");
			String id = data.path("id").asText(null);
			if (id == null || id.isBlank()) {
				throw new IllegalStateException("X create post response missing data.id");
			}
			return new CreatedPost(id, "https://x.com/i/web/status/" + id);
		} catch (RestClientResponseException e) {
			throw new IllegalStateException("X create post failed: HTTP " + e.getStatusCode().value() + " "
					+ truncate(e.getResponseBodyAsString()), e);
		} catch (Exception e) {
			throw new IllegalStateException("X create post failed: " + e.getMessage(), e);
		}
	}

	private void ensureConfigured() {
		if (!properties.isConfigured()) {
			throw new IllegalStateException("X sync is not configured");
		}
	}

	private OAuth1Signer signer() {
		return new OAuth1Signer(properties.getApiKey(), properties.getApiSecret(), properties.getAccessToken(),
				properties.getAccessTokenSecret());
	}

	private static String truncate(String value) {
		if (value == null) {
			return "";
		}
		return value.length() <= 500 ? value : value.substring(0, 500);
	}

	public record CreatedPost(String id, String url) {
	}
}
