package space.nebula.nexus.config;

import java.time.Duration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Type-safe settings for external-link health scans. */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.link-health")
@Validated
public class LinkHealthProperties {

	/** Number of friend links loaded per database page. */
	@Min(1)
	private int friendPageSize = 100;

	/** Number of published posts loaded per database page. */
	@Min(1)
	private int postPageSize = 50;

	/** Timeout applied to each outbound link request. */
	@NotNull
	private Duration requestTimeout = Duration.ofSeconds(5);
}
