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
import space.nebula.nexus.security.exception.CustomAccessDeniedHandler;
import space.nebula.nexus.security.exception.CustomAuthenticationEntryPoint;
import space.nebula.nexus.security.filter.JwtAuthenticationFilter;
import space.nebula.nexus.security.handler.OAuth2AuthenticationSuccessHandler;
import space.nebula.nexus.security.service.CustomOAuth2UserService;

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
	private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;

	/**
	 * Configure BCrypt as our password encoder.
	 */
	@Bean
	public PasswordEncoder passwordEncoder()
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
				// Disable CSRF since we are using stateless JWT authentication
				.csrf(AbstractHttpConfigurer::disable)

				// Set session management to stateless
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// Handle authorization exceptions (401 and 403)
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))

				// Configure URL access rules
				.authorizeHttpRequests(authorize -> authorize
						// Permit all access to Auth APIs, Swagger UI, and Public APIs
						.requestMatchers("/api/v1/auth/**", "/api/v1/public/**", "/v3/api-docs/**", "/swagger-ui/**",
								"/swagger-ui.html", "/login/oauth2/**")
						.permitAll()
						// Protect actuator management endpoints
						.requestMatchers("/management/**").hasRole("ADMIN")
						// All other requests must be authenticated
						.anyRequest().authenticated())

				// Configure OAuth2 Login
				.oauth2Login(
						oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
								.successHandler(oauth2AuthenticationSuccessHandler))

				// Add our custom JWT filter before the standard
				// UsernamePasswordAuthenticationFilter
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
