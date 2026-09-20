package space.nebula.nexus.integration.x;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Minimal OAuth 1.0a HMAC-SHA1 signer for the X API (user-context writes).
 */
public final class OAuth1Signer {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final String consumerKey;
	private final String consumerSecret;
	private final String token;
	private final String tokenSecret;

	public OAuth1Signer(String consumerKey, String consumerSecret, String token, String tokenSecret) {
		this.consumerKey = consumerKey;
		this.consumerSecret = consumerSecret;
		this.token = token;
		this.tokenSecret = tokenSecret;
	}

	public String authorizationHeader(String method, String url, Map<String, String> extraParams) {
		String nonce = UUID.randomUUID().toString().replace("-", "") + Integer.toHexString(RANDOM.nextInt());
		String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

		TreeMap<String, String> params = new TreeMap<>();
		params.put("oauth_consumer_key", consumerKey);
		params.put("oauth_nonce", nonce);
		params.put("oauth_signature_method", "HMAC-SHA1");
		params.put("oauth_timestamp", timestamp);
		params.put("oauth_token", token);
		params.put("oauth_version", "1.0");
		if (extraParams != null) {
			params.putAll(extraParams);
		}

		String parameterString = params.entrySet().stream()
				.map(entry -> percentEncode(entry.getKey()) + "=" + percentEncode(entry.getValue()))
				.collect(Collectors.joining("&"));

		String baseString = method.toUpperCase() + "&" + percentEncode(url) + "&" + percentEncode(parameterString);
		String signingKey = percentEncode(consumerSecret) + "&" + percentEncode(tokenSecret);
		String signature = hmacSha1(baseString, signingKey);

		TreeMap<String, String> oauthParams = new TreeMap<>();
		oauthParams.put("oauth_consumer_key", consumerKey);
		oauthParams.put("oauth_nonce", nonce);
		oauthParams.put("oauth_signature", signature);
		oauthParams.put("oauth_signature_method", "HMAC-SHA1");
		oauthParams.put("oauth_timestamp", timestamp);
		oauthParams.put("oauth_token", token);
		oauthParams.put("oauth_version", "1.0");

		return "OAuth " + oauthParams.entrySet().stream()
				.map(entry -> percentEncode(entry.getKey()) + "=\"" + percentEncode(entry.getValue()) + "\"")
				.collect(Collectors.joining(", "));
	}

	private static String hmacSha1(String data, String key) {
		try {
			Mac mac = Mac.getInstance("HmacSHA1");
			mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
			return Base64.getEncoder().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new IllegalStateException("Unable to sign OAuth 1.0a request", e);
		}
	}

	static String percentEncode(String value) {
		return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20")
				.replace("*", "%2A").replace("%7E", "~");
	}
}
