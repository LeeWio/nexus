package space.nebula.nexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/** Type-safe HTTP security settings that vary between deployments. */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.web-security")
public class WebSecurityProperties {

	/** Browser origins allowed to call the API with credentials. */
	private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
}
