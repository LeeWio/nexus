package space.nebula.nexus.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Configures OpenAPI metadata when API documentation is explicitly enabled.
 */
@Configuration
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", havingValue = "true")
@OpenAPIDefinition(info = @Info(title = "Nexus API", version = "v1.2", description = "### Professional Blog & CMS Backend \n"
		+ "Nexus provides a robust foundation for modern web applications with built-in: \n"
		+ "- **Full-Link Tracing** (TraceId in every response) \n" + "- **Asynchronous Auditing** \n"
		+ "- **Automated Image Processing** (Thumbnails, WebP) \n"
		+ "- **Multi-Driver Storage** (Local, Aliyun OSS, S3)", contact = @Contact(name = "Nebula Space Team", url = "https://nebula.space"), license = @License(name = "MIT License", url = "https://opensource.org/licenses/MIT")), security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", description = "Enter JWT Bearer token obtained from `/api/v1/auth/login`", scheme = "bearer", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {
}
