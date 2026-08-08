package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.validator.UserValidator;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.OtpLoginRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.model.SecurityUser;
import space.nebula.nexus.security.service.LoginSecurityService;
import space.nebula.nexus.security.token.RevokedTokenStore;
import space.nebula.nexus.security.token.RefreshTokenStore;
import space.nebula.nexus.security.util.JwtUtils;
import space.nebula.nexus.utils.RedisUtil;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private RoleRepository roleRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private JwtUtils jwtUtils;
	@Mock
	private UserValidator userValidator;
	@Mock
	private LoginSecurityService loginSecurityService;
	@Mock
	private RedisUtil redisUtil;
	@Mock
	private RevokedTokenStore revokedTokenStore;
	@Mock
	private RefreshTokenStore refreshTokenStore;
	@Mock
	private RabbitTemplate rabbitTemplate;
	@Mock
	private space.nebula.nexus.config.AuthProperties authProperties;
	@Mock
	private space.nebula.nexus.security.config.JwtProperties jwtProperties;

	@InjectMocks
	private AuthServiceImpl authService;

	private RegisterRequest registerRequest;
	private LoginRequest loginRequest;

	@BeforeEach
	void setUp() {
		lenient().when(authProperties.getDefaultRoleCode()).thenReturn("ROLE_USER");
		lenient().when(jwtProperties.getHeader()).thenReturn("Authorization");
		lenient().when(jwtProperties.getPrefix()).thenReturn("Bearer ");
		lenient().when(jwtUtils.extractTokenId(anyString())).thenReturn("refresh-id");

		registerRequest = new RegisterRequest("testuser", "test@example.com", "Password123!");
		loginRequest = new LoginRequest("testuser", "Password123!");
	}

	@Test
	@DisplayName("Should successfully register a new user account")
	void registerAccount_Success() {
		// Arrange
		Role userRole = new Role();
		userRole.setCode("ROLE_USER");
		when(roleRepository.findByCode("ROLE_USER")).thenReturn(Optional.of(userRole));
		when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

		// Act
		ApiResponse<Void> response = authService.registerAccount(registerRequest);

		// Assert
		assertEquals(200, response.code());
		verify(userValidator).validateRegistration(registerRequest);
		verify(userRepository).save(any(User.class));
	}

	@Test
	@DisplayName("Should authenticate successfully and reset failure count")
	void authenticate_Success() {
		// Arrange
		Authentication auth = mock(Authentication.class);
		User user = new User();
		user.setUsername("testuser");
		user.setEmail("test@example.com");
		SecurityUser securityUser = new SecurityUser(user);

		when(auth.getPrincipal()).thenReturn(securityUser);
		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
		when(jwtUtils.generateAccessToken(any())).thenReturn("mock-token");

		// Act
		ApiResponse<AuthResponse> response = authService.authenticate(loginRequest);

		// Assert
		assertEquals(200, response.code());
		assertEquals("mock-token", response.data().accessToken());
		verify(loginSecurityService).validateLoginLock("testuser");
		verify(loginSecurityService).resetLoginFailure("testuser");
	}

	@Test
    @DisplayName("Should record failure on bad credentials during authentication")
    void authenticate_BadCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.authenticate(loginRequest));
        assertEquals(40102, exception.getCode());
        verify(loginSecurityService).recordLoginFailure("testuser");
    }

	@Test
	@DisplayName("Should fail authentication if account is locked")
	void authenticate_AccountLocked() {
		// Arrange
		doThrow(new BusinessException(403, "Account locked")).when(loginSecurityService).validateLoginLock("testuser");

		// Act & Assert
		BusinessException exception = assertThrows(BusinessException.class,
				() -> authService.authenticate(loginRequest));
		assertEquals(403, exception.getCode());
	}

	@Test
	@DisplayName("Should revoke the access token on logout")
	void logout_RevokesAccessToken() {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
		when(jwtUtils.getAccessTokenExpiration()).thenReturn(120_000L);

		authService.logout(request);

		verify(revokedTokenStore).revoke("access-token", Duration.ofMinutes(2));
	}

	@Test
	@DisplayName("Should reject an access token at the refresh endpoint")
	void refreshToken_RejectsAccessToken() {
		when(jwtUtils.isRefreshToken("access-token")).thenReturn(false);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> authService.refreshToken("access-token"));

		assertEquals(BusinessCode.INVALID_TOKEN.getCode(), exception.getCode());
		verifyNoInteractions(refreshTokenStore);
	}

	@Test
	@DisplayName("Should send OTP and keep it in Redis when email dispatch succeeds")
	void sendOtp_Success() {
		User user = new User();
		user.setEmail("test@example.com");
		when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(redisUtil.set(eq(CacheConstants.OTP_CODE + "test@example.com"), any(String.class), eq(5L),
				eq(TimeUnit.MINUTES))).thenReturn(true);

		ApiResponse<Void> response = authService.sendOtp("test@example.com");

		assertEquals(200, response.code());
		assertEquals("If an account is associated with this email, an OTP has been sent.", response.message());
		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.MAIL_EXCHANGE), eq(RabbitMQConfig.MAIL_ROUTING_KEY),
				any(Object.class));
		verify(redisUtil, never()).delete(anyString());
	}

	@Test
	@DisplayName("Should not reveal whether an OTP email belongs to an account")
	void sendOtp_UnknownEmailReturnsTheSameAcknowledgement() {
		when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

		ApiResponse<Void> response = authService.sendOtp("unknown@example.com");

		assertEquals(200, response.code());
		assertEquals("If an account is associated with this email, an OTP has been sent.", response.message());
		verifyNoInteractions(redisUtil, rabbitTemplate);
	}

	@Test
	@DisplayName("Should atomically consume OTP during login")
	void loginWithOtp_ConsumesOtpAtomically() {
		String email = "test@example.com";
		String otpKey = CacheConstants.OTP_CODE + email;
		User user = new User();
		user.setUsername("testuser");
		user.setEmail(email);
		user.setStatus(UserStatus.ACTIVE);
		when(redisUtil.consumeIfEquals(otpKey, "123456")).thenReturn(true);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
		when(jwtUtils.generateAccessToken(any())).thenReturn("access-token");
		when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh-token");

		ApiResponse<AuthResponse> response = authService.loginWithOtp(new OtpLoginRequest(email, "123456"));

		assertEquals(200, response.code());
		verify(redisUtil).consumeIfEquals(otpKey, "123456");
		verify(redisUtil, never()).get(eq(otpKey), eq(String.class));
		verify(redisUtil, never()).delete(otpKey);
	}

	@Test
	@DisplayName("Should reject an invalid OTP without loading the user")
	void loginWithOtp_RejectsInvalidOtp() {
		String email = "test@example.com";
		when(redisUtil.consumeIfEquals(CacheConstants.OTP_CODE + email, "000000")).thenReturn(false);

		BusinessException exception = assertThrows(BusinessException.class,
				() -> authService.loginWithOtp(new OtpLoginRequest(email, "000000")));

		assertEquals(BusinessCode.INVALID_TOKEN.getCode(), exception.getCode());
		verifyNoInteractions(userRepository);
	}

	@Test
	@DisplayName("Should acknowledge OTP requests when Redis cannot store the code")
	void sendOtp_RedisFailureReturnsAcknowledgement() {
		User user = new User();
		user.setEmail("test@example.com");
		String otpKey = CacheConstants.OTP_CODE + "test@example.com";
		when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(redisUtil.set(eq(otpKey), any(String.class), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(false);

		ApiResponse<Void> response = authService.sendOtp("test@example.com");

		assertEquals(200, response.code());
		assertEquals("If an account is associated with this email, an OTP has been sent.", response.message());
		verifyNoInteractions(rabbitTemplate);
	}

	@Test
	@DisplayName("Should delete OTP and acknowledge requests when email dispatch fails")
	void sendOtp_MailFailureReturnsAcknowledgement() {
		User user = new User();
		user.setEmail("test@example.com");
		String otpKey = CacheConstants.OTP_CODE + "test@example.com";
		when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
		when(redisUtil.set(eq(otpKey), any(String.class), eq(5L), eq(TimeUnit.MINUTES))).thenReturn(true);
		doThrow(new RuntimeException("RabbitMQ connection failed")).when(rabbitTemplate).convertAndSend(
				eq(RabbitMQConfig.MAIL_EXCHANGE), eq(RabbitMQConfig.MAIL_ROUTING_KEY), any(Object.class));

		ApiResponse<Void> response = authService.sendOtp("test@example.com");

		assertEquals(200, response.code());
		assertEquals("If an account is associated with this email, an OTP has been sent.", response.message());
		verify(redisUtil).delete(otpKey);
	}
}
