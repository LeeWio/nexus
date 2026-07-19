package space.nebula.nexus.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Redirects OAuth2 failures to the frontend with a stable, user-facing error code.
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler
{
	@Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/redirect}")
	private String redirectUri;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException
	{
		String errorCode = exception instanceof OAuth2AuthenticationException oauthException
				? oauthException.getError().getErrorCode() : "oauth_login_failed";
		String targetUrl = UriComponentsBuilder.fromUriString(redirectUri).queryParam("error", errorCode).build()
				.toUriString();
		if (request.getSession(false) != null)
		{
			request.getSession(false).invalidate();
		}
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}
}
