package space.nebula.nexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Site-owner X (Twitter) API credentials and feature flag. Tokens stay on the
 * server; never expose them to Odyssey or the browser.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.x")
public class XProperties {

	/** Master switch. When false, shareToX requests are ignored. */
	private boolean enabled = false;

	private String apiKey = "";
	private String apiSecret = "";
	private String accessToken = "";
	private String accessTokenSecret = "";

	/** Max visible characters for the tweet body (X free-tier style limit). */
	private int maxTextLength = 280;

	/** Max images attached to a single post (X hard cap). */
	private int maxImages = 4;

	public boolean isConfigured() {
		return enabled && hasText(apiKey) && hasText(apiSecret) && hasText(accessToken) && hasText(accessTokenSecret);
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
