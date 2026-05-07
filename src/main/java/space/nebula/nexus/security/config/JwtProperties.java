package space.nebula.nexus.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    
    /**
     * The secret key used to sign the JWT. Should be a long, random string (at least 256 bits).
     */
    private String secret;

    /**
     * Access token expiration time in milliseconds. Default 2 hours.
     */
    private long accessTokenExpiration = 7200000;

    /**
     * Refresh token expiration time in milliseconds. Default 7 days.
     */
    private long refreshTokenExpiration = 604800000;

    /**
     * The HTTP header name where the JWT is expected. Default "Authorization".
     */
    private String header = "Authorization";

    /**
     * The prefix for the JWT value in the header. Default "Bearer ".
     */
    private String prefix = "Bearer ";
}
