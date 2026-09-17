package space.nebula.nexus.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.model.SecurityUser;
import space.nebula.nexus.security.service.OAuthAccountResolver;
import space.nebula.nexus.security.token.OAuthLoginCodeStore;
import space.nebula.nexus.service.IAuthService;

import java.io.IOException;

/**
 * Completes OAuth2 login by issuing tokens server-side and redirecting the
 * browser with a short-lived opaque {@code code}. The SPA exchanges that code
 * for tokens over HTTPS JSON so JWTs never appear in query strings, history, or
 * Referer headers.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	private final UserRepository userRepository;
	private final IAuthService authService;
	private final OAuthLoginCodeStore oauthLoginCodeStore;

	@Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
	private String redirectUri;

	public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, @Lazy IAuthService authService,
			OAuthLoginCodeStore oauthLoginCodeStore) {
		this.userRepository = userRepository;
		this.authService = authService;
		this.oauthLoginCodeStore = oauthLoginCodeStore;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		Object localUserId = oAuth2User.getAttributes().get(OAuthAccountResolver.LOCAL_USER_ID_ATTRIBUTE);
		if (localUserId == null) {
			throw new ServletException("OAuth login did not resolve a local user account");
		}

		User user = userRepository.findById(Long.valueOf(String.valueOf(localUserId)))
				.orElseThrow(() -> new ServletException("User not found after OAuth login"));

		AuthResponse authResponse = authService.issueTokens(new SecurityUser(user));
		String loginCode = oauthLoginCodeStore.issue(authResponse);

		String targetUrl = UriComponentsBuilder.fromUriString(redirectUri).queryParam("code", loginCode).build()
				.toUriString();
		if (request.getSession(false) != null) {
			request.getSession(false).invalidate();
		}

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
