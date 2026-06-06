package space.nebula.nexus.security.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.security.config.JwtProperties;
import space.nebula.nexus.security.util.JwtUtils;
import space.nebula.nexus.utils.RedisUtil;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter
{

	public static final String SECURITY_ERROR_CODE = "NEXUS_SECURITY_ERROR_CODE";

	@Resource
	private JwtUtils jwtUtils;

	@Resource
	@org.springframework.context.annotation.Lazy
	private UserDetailsService userDetailsService;

	@Resource
	private JwtProperties jwtProperties;

	@Resource
	private RedisUtil redisUtil;

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException
	{

		final String authHeader = request.getHeader(jwtProperties.getHeader());
		final String jwt;
		final String username;

		// 1. Check if the Authorization header is present and correctly formatted
		if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(jwtProperties.getPrefix()))
		{
			filterChain.doFilter(request, response);
			return;
		}

		try
		{
			// 2. Extract the token
			jwt = authHeader.substring(jwtProperties.getPrefix().length());

			// 3. Check Blacklist (Redis)
			String blacklistKey = "nexus:jwt:blacklist:" + jwt;
			if (redisUtil.hasKey(blacklistKey))
			{
				log.warn("Rejected blacklisted JWT token");
				request.setAttribute(SECURITY_ERROR_CODE, BusinessCode.INVALID_TOKEN);
				filterChain.doFilter(request, response);
				return;
			}

			// 4. Extract the username from the token
			username = jwtUtils.extractUsername(jwt);

			// 5. If the username is valid and the user is not already authenticated in the
			// SecurityContext
			if (username != null && SecurityContextHolder.getContext().getAuthentication() == null)
			{
				// Load the user details from the database
				UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

				// 5. Validate the token
				if (jwtUtils.isTokenValid(jwt, userDetails))
				{
					// Create an Authentication token
					UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
							null, // We don't need credentials after successful authentication
							userDetails.getAuthorities());

					// Attach details of the HTTP request to the auth token (e.g., IP address,
					// session ID)
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

					// 6. Update the SecurityContextHolder to signify the user is authenticated
					SecurityContextHolder.getContext().setAuthentication(authToken);
					log.debug("Successfully authenticated user '{}' with JWT.", username);
				}
			}
		}
		catch (ExpiredJwtException e)
		{
			log.warn("JWT token is expired: {}", e.getMessage());
			request.setAttribute(SECURITY_ERROR_CODE, BusinessCode.INVALID_TOKEN);
		}
		catch (SignatureException | MalformedJwtException | UnsupportedJwtException e)
		{
			log.warn("JWT token is invalid: {}", e.getMessage());
			request.setAttribute(SECURITY_ERROR_CODE, BusinessCode.INVALID_TOKEN);
		}
		catch (Exception e)
		{
			log.error("Authentication internal error: {}", e.getMessage());
			request.setAttribute(SECURITY_ERROR_CODE, BusinessCode.UNAUTHORIZED);
		}

		// 7. Continue down the filter chain
		filterChain.doFilter(request, response);
	}
}
