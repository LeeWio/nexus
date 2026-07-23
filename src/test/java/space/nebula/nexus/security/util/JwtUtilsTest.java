package space.nebula.nexus.security.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import space.nebula.nexus.security.config.JwtProperties;
import space.nebula.nexus.security.model.SecurityUser;
import space.nebula.nexus.enums.UserStatus;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

	@Mock
	private JwtProperties jwtProperties;

	private JwtUtils jwtUtils;

	private UserDetails userDetails;

	@BeforeEach
	void setUp() {
		lenient().when(jwtProperties.getSecret())
				.thenReturn("4A614E645267556B58703273357638792F423F4528482B4D6251655468576D5A");
		lenient().when(jwtProperties.getAccessTokenExpiration()).thenReturn(7200000L);
		lenient().when(jwtProperties.getRefreshTokenExpiration()).thenReturn(604800000L);

		jwtUtils = new JwtUtils(jwtProperties);
		userDetails = new User("testuser", "password", Collections.emptyList());
	}

	@Test
	void generateAndValidateToken() {
		String token = jwtUtils.generateAccessToken(userDetails);
		assertNotNull(token);

		String username = jwtUtils.extractUsername(token);
		assertEquals("testuser", username);

		assertTrue(jwtUtils.isTokenValid(token, userDetails));
		assertTrue(jwtUtils.isAccessToken(token));
		assertFalse(jwtUtils.isRefreshToken(token));
		assertNotNull(jwtUtils.extractTokenId(token));

		String refreshToken = jwtUtils.generateRefreshToken(userDetails);
		assertTrue(jwtUtils.isRefreshToken(refreshToken));
		assertFalse(jwtUtils.isAccessToken(refreshToken));
	}

	@Test
	void rejectsTokenAfterSecurityVersionChanges() {
		space.nebula.nexus.entity.User user = new space.nebula.nexus.entity.User();
		user.setUsername("testuser");
		user.setPassword("password");
		user.setStatus(UserStatus.ACTIVE);
		SecurityUser securityUser = new SecurityUser(user);
		String token = jwtUtils.generateAccessToken(securityUser);

		user.setTokenVersion(1);

		assertFalse(jwtUtils.isTokenValid(token, securityUser));
	}

	@Test
	void rejectsTokenForDisabledAccount() {
		space.nebula.nexus.entity.User user = new space.nebula.nexus.entity.User();
		user.setUsername("testuser");
		user.setPassword("password");
		user.setStatus(UserStatus.ACTIVE);
		SecurityUser securityUser = new SecurityUser(user);
		String token = jwtUtils.generateAccessToken(securityUser);

		user.setStatus(UserStatus.INACTIVE);

		assertFalse(jwtUtils.isTokenValid(token, securityUser));
	}
}
