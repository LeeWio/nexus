package space.nebula.nexus.security.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import space.nebula.nexus.security.exception.CustomAccessDeniedHandler;
import space.nebula.nexus.security.exception.CustomAuthenticationEntryPoint;
import space.nebula.nexus.security.filter.JwtAuthenticationFilter;
import space.nebula.nexus.security.handler.OAuth2AuthenticationSuccessHandler;
import space.nebula.nexus.security.handler.OAuth2AuthenticationFailureHandler;
import space.nebula.nexus.security.service.CustomOAuth2UserService;
import space.nebula.nexus.security.service.CustomOidcUserService;
import space.nebula.nexus.config.WebSecurityProperties;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Enables @PreAuthorize, @Secured, etc.
@RequiredArgsConstructor
public class SecurityConfig
{

	private final CustomAuthenticationEntryPoint authenticationEntryPoint;
	private final CustomAccessDeniedHandler accessDeniedHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomOAuth2UserService customOAuth2UserService;
	private final CustomOidcUserService customOidcUserService;
	private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
	private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;
	private final WebSecurityProperties webSecurityProperties;

	/**
	 * Configure BCrypt as our password encoder.
	 */
	@Bean
	public static PasswordEncoder passwordEncoder()
	{
		return new BCryptPasswordEncoder();
	}

	/**
	 * Configure the global AuthenticationManager which manages the authentication
	 * providers.
	 */
	@Bean
	public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder)
	{
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
		authenticationProvider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(authenticationProvider);
	}

	/**
	 * The core SecurityFilterChain that handles HTTP security rules.
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
	{
		http
				// 1. Explicit CORS configuration
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))

				// 2. Disable CSRF since we are using stateless JWT authentication
				.csrf(AbstractHttpConfigurer::disable)

				// 3. Enhance Security Headers
				.headers(headers -> headers
						// Prevent clickjacking
						.frameOptions(frame -> frame.deny())
						// Enable HSTS (Strict-Transport-Security)
						.httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31536000))
						// Content-Security-Policy (CSP) - Basic hardening
						.contentSecurityPolicy(csp -> csp.policyDirectives(
								"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none';")))

				// 4. Configure session management for JWT APIs and OAuth2 state handling
				// OAuth2 authorization requires a short-lived session for the state parameter.
				// The success handler invalidates it immediately after issuing the JWT.
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

				// 5. Handle authorization exceptions (401 and 403)
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))

				// 6. Configure URL access rules
				.authorizeHttpRequests(authorize -> authorize
						// Permit all access to Auth APIs, Swagger UI, and Public APIs
						.requestMatchers("/api/v1/auth/**", "/api/v1/public/**", "/v3/api-docs/**", "/swagger-ui/**",
								"/swagger-ui.html", "/oauth2/**", "/login/oauth2/**", "/livez", "/readyz")
						.permitAll()
						// Protect actuator management endpoints
						.requestMatchers("/management/**").hasRole("ADMIN")
						// All other requests must be authenticated
						.anyRequest().authenticated())

				// 7. Configure OAuth2 Login
				.oauth2Login(
						oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)
								.oidcUserService(customOidcUserService))
								.successHandler(oauth2AuthenticationSuccessHandler)
								.failureHandler(oauth2AuthenticationFailureHandler))

				// 8. Add our custom JWT filter before the standard
				// UsernamePasswordAuthenticationFilter
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/**
	 * Standardized CORS configuration for a secure production environment.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource()
	{
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(webSecurityProperties.getAllowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id", "X-Requested-With",
				"Idempotency-Key"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L); // Cache preflight for 1 hour

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
