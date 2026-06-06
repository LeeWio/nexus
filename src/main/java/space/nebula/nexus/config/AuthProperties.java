package space.nebula.nexus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /**
     * Maximum number of login failures before account lock.
     */
    private int maxLoginFailures = 5;

    /**
     * Duration of account lock in minutes.
     */
    private long lockDurationMinutes = 15;

    /**
     * Default role code for new users.
     */
    private String defaultRoleCode = "ROLE_USER";
}
