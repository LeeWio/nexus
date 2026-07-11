package space.nebula.nexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Type-safe newsletter workflow and delivery settings. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.newsletter")
public class NewsletterProperties {

	/** Public application URL used to generate email links. */
	private String baseUrl = "http://localhost:8080";

	/** Maximum lifetime of a subscription verification link. */
	private Duration verificationTtl = Duration.ofHours(24);

	/** Number of subscribers loaded for each broadcast page. */
	private int batchSize = 200;
}
