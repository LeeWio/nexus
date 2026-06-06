package space.nebula.nexus.security.exception;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.security.filter.JwtAuthenticationFilter;

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

		// 1. Check if a descriptive error code was set by JwtAuthenticationFilter
		Object securityCodeObj = request.getAttribute(JwtAuthenticationFilter.SECURITY_ERROR_CODE);
		BusinessCode businessCode = (securityCodeObj instanceof BusinessCode bc) ? bc : BusinessCode.UNAUTHORIZED;

		// 2. Use the descriptive message if available, otherwise fallback to the
		// generic one
		String message = (securityCodeObj != null) ? businessCode.getMessage() : authException.getMessage();

		response.getWriter().write(String.format("{\"code\": %d, \"message\": \"%s\", \"data\": null}",
				businessCode.getCode(), message));
	}
}
