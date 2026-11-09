package space.nebula.nexus.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/** Privacy and retention controls for first-party analytics. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.analytics")
@Validated
public class AnalyticsProperties {

	/** Secret pepper used to derive a non-reversible visitor identifier. */
	@NotBlank
	private String hashSalt;

	/**
	 * Number of days raw, anonymized request and engagement events are retained.
	 */
	@Min(1)
	private int retentionDays = 90;

	/** Lifetime of the anonymous first-party browser session cookie. */
	@Min(1)
	private int sessionCookieMaxAgeDays = 30;

	/**
	 * Visitor hashes that belong to the site owner. Their page views stay in the
	 * log but are excluded from reader totals unless the dashboard asks for them.
	 */
	private List<String> ownerHashes = new ArrayList<>();

	/** Repeat views of the same path inside one session collapse for this long. */
	@Min(1)
	private int pageViewDedupeMinutes = 30;
}
