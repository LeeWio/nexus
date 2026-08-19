package space.nebula.nexus.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import space.nebula.nexus.security.config.JwtProperties;
import space.nebula.nexus.security.model.SecurityUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtils {

	private final JwtProperties jwtProperties;
	private final SecretKey key;

	public JwtUtils(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
		byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		this.key = Keys.hmacShaKeyFor(keyBytes);
	}

	/**
	 * Extracts the username (subject) from the token.
	 */
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	/**
	 * Extracts an arbitrary claim from the token.
	 */
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	/**
	 * Generates an access token for the given user.
	 */
	public String generateAccessToken(UserDetails userDetails) {
		return buildToken(userDetails, jwtProperties.getAccessTokenExpiration(), "access");
	}

	/**
	 * Generates a refresh token for the given user.
	 */
	public String generateRefreshToken(UserDetails userDetails) {
		return buildToken(userDetails, jwtProperties.getRefreshTokenExpiration(), "refresh");
	}

	public long getAccessTokenExpiration() {
		return jwtProperties.getAccessTokenExpiration();
	}

	public long getRefreshTokenExpiration() {
		return jwtProperties.getRefreshTokenExpiration();
	}

	public String extractTokenId(String token) {
		return extractClaim(token, Claims::getId);
	}

	public boolean isAccessToken(String token) {
		return "access".equals(extractClaim(token, claims -> claims.get("token_type", String.class)));
	}

	public boolean isRefreshToken(String token) {
		return "refresh".equals(extractClaim(token, claims -> claims.get("token_type", String.class)));
	}

	private String buildToken(UserDetails userDetails, long expiration, String tokenType) {
		return Jwts.builder().id(UUID.randomUUID().toString()).subject(userDetails.getUsername())
				.claim("token_type", tokenType).claim("token_version", tokenVersion(userDetails))
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis() + expiration)).signWith(key).compact();
	}

	/**
	 * Validates the token against the user details and checks expiration.
	 */
	public boolean isTokenValid(String token, UserDetails userDetails) {
		try {
			final Claims claims = extractAllClaims(token);
			final String username = claims.getSubject();
			Number versionClaim = claims.get("token_version", Number.class);
			Date expiration = claims.getExpiration();
			boolean isExpired = expiration != null && expiration.before(new Date());

			return username != null && username.equals(userDetails.getUsername()) && versionClaim != null
					&& versionClaim.intValue() == tokenVersion(userDetails) && userDetails.isEnabled()
					&& userDetails.isAccountNonLocked() && userDetails.isAccountNonExpired()
					&& userDetails.isCredentialsNonExpired() && !isExpired;
		} catch (Exception e) {
			log.error("Token validation failed: {}", e.getMessage());
			return false;
		}
	}

	private int tokenVersion(UserDetails userDetails) {
		return userDetails instanceof SecurityUser securityUser ? securityUser.getUser().getTokenVersion() : 0;
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		try {
			return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		} catch (SignatureException e) {
			log.error("Invalid JWT signature: {}", e.getMessage());
			throw e;
		} catch (MalformedJwtException e) {
			log.error("Invalid JWT token: {}", e.getMessage());
			throw e;
		} catch (ExpiredJwtException e) {
			log.error("JWT token is expired: {}", e.getMessage());
			throw e;
		} catch (UnsupportedJwtException e) {
			log.error("JWT token is unsupported: {}", e.getMessage());
			throw e;
		} catch (IllegalArgumentException e) {
			log.error("JWT claims string is empty: {}", e.getMessage());
			throw e;
		}
	}
}
