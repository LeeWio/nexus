package space.nebula.nexus.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

import lombok.RequiredArgsConstructor;

/**
 * Configuration to register the custom PermissionEvaluator with Spring Security.
 */
@Configuration
@RequiredArgsConstructor
public class MethodSecurityConfig {

	private final NexusPermissionEvaluator permissionEvaluator;

	@Bean
	public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
		DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
		expressionHandler.setPermissionEvaluator(permissionEvaluator);
		return expressionHandler;
	}
}
