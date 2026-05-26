package space.nebula.nexus.security.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles 403 Forbidden errors (user is authenticated but does not have
 * required permissions). Returns a standardized JSON response instead of Spring
 * Security's default HTML page.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler
{

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException, ServletException
	{
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		// In a real project, you would map this to a common Result/Response class using
		// Jackson (ObjectMapper).
		response.getWriter().write(String.format("{\"code\": 403, \"message\": \"Forbidden: %s\", \"data\": null}",
				accessDeniedException.getMessage()));
	}
}
