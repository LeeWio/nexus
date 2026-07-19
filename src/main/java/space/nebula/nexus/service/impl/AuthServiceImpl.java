package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
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
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.model.SecurityUser;
import space.nebula.nexus.security.token.RevokedTokenStore;
import space.nebula.nexus.security.token.RefreshTokenStore;
import space.nebula.nexus.security.service.LoginSecurityService;
import space.nebula.nexus.security.util.JwtUtils;
import space.nebula.nexus.service.IAuthService;
import space.nebula.nexus.utils.RedisUtil;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Professional implementation of user authentication and account management.
 * Handles secure registration, multi-factor authentication (OTP), and JWT
 * session management. Uses RabbitMQ for asynchronous email delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService
{

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtUtils jwtUtils;
	private final UserValidator userValidator;
	private final LoginSecurityService loginSecurityService;
	private final RedisUtil redisUtil;
	private final RevokedTokenStore revokedTokenStore;
	private final RefreshTokenStore refreshTokenStore;
	private final RabbitTemplate rabbitTemplate;
	private final space.nebula.nexus.config.AuthProperties authProperties;
	private final space.nebula.nexus.security.config.JwtProperties jwtProperties;

	@Override
	@Transactional
	@LogOperation("User Registration")
	public ApiResponse<Void> registerAccount(RegisterRequest request)
	{
		userValidator.validateRegistration(request);

		var newUser = createNewUser(request);
		assignDefaultRole(newUser);

		userRepository.save(newUser);
		log.info("User account registered successfully, pending audit: {}", newUser.getUsername());

		// Prepare email variables using modern Java Map.of
		Map<String, Object> emailVars = Map.of(
				"username", newUser.getUsername(),
				"message", "Your account is awaiting administrator approval."
		);

		// Async Welcome/Pending Email
		TemplateMailMessage welcomeMail = TemplateMailMessage.builder()
				.to(newUser.getEmail())
				.subject("Nexus Registration Received")
				.templateName("otp-login")
				.variables(emailVars)
				.type(TemplateMailMessage.MailType.TEMPLATE)
				.build();

		try
		{
			rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, welcomeMail);
		}
		catch (Exception e)
		{
			log.error("Failed to dispatch registration email for: {}", newUser.getEmail());
		}

		return ApiResponse.success("Registration successful. Your account is awaiting approval.", null);
	}

	@Override
	@LogOperation("User Login")
	public ApiResponse<AuthResponse> authenticate(LoginRequest request)
	{
		var username = request.username();

		loginSecurityService.validateLoginLock(username);

		try
		{
			var authInput = new UsernamePasswordAuthenticationToken(username, request.password());
			var authentication = authenticationManager.authenticate(authInput);

			loginSecurityService.resetLoginFailure(username);
			SecurityContextHolder.getContext().setAuthentication(authentication);

			var securityUser = (SecurityUser) authentication.getPrincipal();
			log.info("User authenticated successfully: {}", securityUser.getUsername());

			return ApiResponse.success("Login successful", createAuthResponse(securityUser));
		}
		catch (BadCredentialsException e)
		{
			loginSecurityService.recordLoginFailure(username);
			throw new BusinessException(BusinessCode.BAD_CREDENTIALS);
		}
		catch (BusinessException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			log.error("Unexpected authentication error for user: {}", username, e);
			throw new BusinessException(BusinessCode.ERROR, "Authentication service is temporarily unavailable");
		}
	}

	@Override
	@LogOperation("Send Login OTP")
	public ApiResponse<Void> sendOtp(String email)
	{
			userRepository.findByEmail(email).orElseThrow(
					() -> new BusinessException(BusinessCode.USER_NOT_FOUND, "No account is linked to this email"));

		var otp = RandomUtil.randomNumbers(6);
		var otpKey = CacheConstants.OTP_CODE + email;
			Assert.isTrue(redisUtil.set(otpKey, otp, 5, TimeUnit.MINUTES),
					() -> new BusinessException(BusinessCode.ERROR, "Failed to store the OTP code"));

		Map<String, Object> variables = Dict.create().set("otp", otp).set("expireMin", 5);

		TemplateMailMessage mailMessage = TemplateMailMessage.builder().to(email).subject("Nexus Login OTP")
				.templateName("otp-login").variables(variables).type(TemplateMailMessage.MailType.TEMPLATE).build();

		try
		{
			rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, mailMessage);
			log.info("OTP task dispatched to MQ for {}", email);
		}
		catch (Exception e)
		{
			redisUtil.delete(otpKey);
			log.error("Failed to dispatch OTP email task to MQ for: {}", email, e);
				throw new BusinessException(BusinessCode.ERROR, "Failed to dispatch the email task");
		}

		return ApiResponse.success("OTP code sent successfully. Please check your inbox.", null);
	}

	@Override
	@Transactional
	@LogOperation("OTP Login")
	public ApiResponse<AuthResponse> loginWithOtp(OtpLoginRequest request)
	{
		var email = request.email();
		var code = request.code();
		var otpKey = CacheConstants.OTP_CODE + email;

			Assert.isTrue(redisUtil.consumeIfEquals(otpKey, code),
					() -> new BusinessException(BusinessCode.INVALID_TOKEN, "Verification code is invalid or expired"));

		var user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));

			Assert.isTrue(user.getStatus() == UserStatus.ACTIVE,
					() -> new BusinessException(BusinessCode.ACCOUNT_DISABLED, "Account is not active"));

		var securityUser = new SecurityUser(user);
		var authentication = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		log.info("User logged in via OTP authentication: {}", user.getUsername());

		return ApiResponse.success("Login successful", createAuthResponse(securityUser));
	}

	@Override
	public ApiResponse<User> getAuthenticatedUser()
	{
		var username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByUsername(username).map(ApiResponse::success)
				.orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND, "Authenticated user could not be resolved"));
	}

	@Override
	public ApiResponse<Void> logout(HttpServletRequest request)
	{
		final String authHeader = request.getHeader(jwtProperties.getHeader());
		if (StrUtil.isNotBlank(authHeader) && authHeader.startsWith(jwtProperties.getPrefix()))
		{
			String token = authHeader.substring(jwtProperties.getPrefix().length());
			// Blacklist token in Redis until it naturally expires
			long remainingTime = jwtUtils.getAccessTokenExpiration();
			revokedTokenStore.revoke(token, java.time.Duration.ofMillis(remainingTime));
			log.info("Token blacklisted for logout");
		}
		SecurityContextHolder.clearContext();
		return ApiResponse.success("Logged out successfully", null);
	}

	@Override
	public ApiResponse<AuthResponse> refreshToken(String refreshToken)
	{
		Assert.notBlank(refreshToken, () -> new BusinessException(BusinessCode.INVALID_TOKEN, "Refresh token is required"));

			Assert.isTrue(jwtUtils.isRefreshToken(refreshToken),
					() -> new BusinessException(BusinessCode.INVALID_TOKEN, "Refresh token is required"));
		String username = jwtUtils.extractUsername(refreshToken);
		var user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));

		Assert.isTrue(user.getStatus() == UserStatus.ACTIVE,
				() -> new BusinessException(BusinessCode.ACCOUNT_DISABLED, "Account is not active"));

		SecurityUser securityUser = new SecurityUser(user);
		if (jwtUtils.isTokenValid(refreshToken, securityUser)
				&& refreshTokenStore.consume(jwtUtils.extractTokenId(refreshToken), username))
		{
			return ApiResponse.success("Token refreshed successfully", createAuthResponse(securityUser));
		}

		throw new BusinessException(BusinessCode.INVALID_TOKEN, "Refresh token is invalid or expired");
	}

	private AuthResponse createAuthResponse(SecurityUser securityUser)
	{
		var accessToken = jwtUtils.generateAccessToken(securityUser);
		var refreshToken = jwtUtils.generateRefreshToken(securityUser);
		refreshTokenStore.issue(jwtUtils.extractTokenId(refreshToken), securityUser.getUsername(),
				java.time.Duration.ofMillis(jwtUtils.getRefreshTokenExpiration()));
		var roles = securityUser.getAuthorities().stream().map(ga -> ga.getAuthority()).collect(Collectors.toSet());

		return AuthResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.username(securityUser.getUsername())
				.email(securityUser.getUser().getEmail())
				.roles(roles)
				.build();
	}

	private User createNewUser(RegisterRequest request)
	{
		var user = new User();
		user.setUsername(request.username());
		user.setEmail(request.email());
		user.setPassword(passwordEncoder.encode(request.password()));
		user.setStatus(UserStatus.PENDING);
		return user;
	}

	private void assignDefaultRole(User user)
	{
		String defaultRoleCode = authProperties.getDefaultRoleCode();
		Role defaultRole = roleRepository.findByCode(defaultRoleCode).orElseGet(() ->
			{
				log.warn("Default '{}' role missing; initializing fallback", defaultRoleCode);
				var newRole = new Role();
				newRole.setName("Standard User");
				newRole.setCode(defaultRoleCode);
				newRole.setDescription("Default role for registered members");
				return roleRepository.save(newRole);
			});
		user.setRoles(Collections.singleton(defaultRole));
	}
}
