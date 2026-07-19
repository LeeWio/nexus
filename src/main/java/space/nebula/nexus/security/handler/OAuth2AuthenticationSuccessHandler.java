package space.nebula.nexus.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.model.SecurityUser;
import space.nebula.nexus.security.util.JwtUtils;
import space.nebula.nexus.security.service.OAuthAccountResolver;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler
{

	private final JwtUtils jwtUtils;
	private final UserRepository userRepository;

	@Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
	private String redirectUri;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException
	{
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		Object localUserId = oAuth2User.getAttributes().get(OAuthAccountResolver.LOCAL_USER_ID_ATTRIBUTE);
		if (localUserId == null)
		{
			throw new ServletException("OAuth login did not resolve a local user account");
		}

		User user = userRepository.findById(Long.valueOf(String.valueOf(localUserId)))
				.orElseThrow(() -> new ServletException("User not found after OAuth login"));

		String token = jwtUtils.generateAccessToken(new SecurityUser(user));

		String targetUrl = UriComponentsBuilder.fromUriString(redirectUri).queryParam("token", token).build()
				.toUriString();
		if (request.getSession(false) != null)
		{
			request.getSession(false).invalidate();
		}

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
