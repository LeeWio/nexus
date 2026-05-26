package space.nebula.nexus.security.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 401 Unauthorized errors (user is not authenticated). Returns a
 * standardized JSON response instead of Spring Security's default HTML page.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint
{

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException
	{
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		// In a real project, you would map this to a common Result/Response class using
		// Jackson (ObjectMapper).
		response.getWriter().write(String.format("{\"code\": 401, \"message\": \"Unauthorized: %s\", \"data\": null}",
				authException.getMessage()));
	}
}
