package space.nebula.nexus.security.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import space.nebula.nexus.security.config.JwtProperties;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilsTest {

	@Mock
	private JwtProperties jwtProperties;

	@InjectMocks
	private JwtUtils jwtUtils;

	private UserDetails userDetails;

	@BeforeEach
    void setUp() {
        when(jwtProperties.getSecret()).thenReturn("4A614E645267556B58703273357638792F423F4528482B4D6251655468576D5A");
        when(jwtProperties.getAccessTokenExpiration()).thenReturn(7200000L);
        
        jwtUtils.init();
        
        userDetails = new User("testuser", "password", Collections.emptyList());
    }

	@Test
	void generateAndValidateToken() {
		String token = jwtUtils.generateAccessToken(userDetails);
		assertNotNull(token);

		String username = jwtUtils.extractUsername(token);
		assertEquals("testuser", username);

		assertTrue(jwtUtils.isTokenValid(token, userDetails));
	}
}
