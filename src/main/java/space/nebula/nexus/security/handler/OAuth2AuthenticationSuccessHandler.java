package space.nebula.nexus.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.model.SecurityUser;
import space.nebula.nexus.security.util.JwtUtils;

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
		DefaultOAuth2User oAuth2User = (DefaultOAuth2User) authentication.getPrincipal();
		String githubId = String.valueOf(oAuth2User.getAttributes().get("id"));

		User user = userRepository.findByGithubId(githubId)
				.orElseThrow(() -> new ServletException("User not found after OAuth login"));

		String token = jwtUtils.generateAccessToken(new SecurityUser(user));

		String targetUrl = UriComponentsBuilder.fromUriString(redirectUri).queryParam("token", token).build()
				.toUriString();

		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
