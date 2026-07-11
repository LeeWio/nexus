package space.nebula.nexus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Optional one-time administrator bootstrap settings. Disabled by default. */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.bootstrap-admin")
public class BootstrapAdminProperties {
	private boolean enabled;
	private String username;
	private String email;
	private String password;
}
