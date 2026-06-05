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
import space.nebula.nexus.security.service.LoginSecurityService;
import space.nebula.nexus.security.util.JwtUtils;
import space.nebula.nexus.service.IAuthService;
import space.nebula.nexus.utils.RedisUtil;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;

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
	private final RabbitTemplate rabbitTemplate;

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

		// Async Welcome/Pending Email
		TemplateMailMessage welcomeMail = TemplateMailMessage.builder().to(newUser.getEmail())
				.subject("Nexus Registration Received").templateName("otp-login") // Ideally a "welcome" template, but
																					// reusing for demo
				.variables(Dict.create().set("username", newUser.getUsername()).set("message",
						"Your account is pending administrator approval."))
				.type(TemplateMailMessage.MailType.TEMPLATE).build();

		try
		{
			rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, welcomeMail);
		}
		catch (Exception e)
		{
			log.error("Failed to dispatch registration email for: {}", newUser.getEmail());
		}

		return ApiResponse.success("Registration successful. Your account is pending administrator approval.", null);
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
			var accessToken = jwtUtils.generateAccessToken(securityUser);

			var roles = securityUser.getAuthorities().stream().map(ga -> ga.getAuthority()).collect(Collectors.toSet());

			var authResponse = AuthResponse.builder().accessToken(accessToken).username(securityUser.getUsername())
					.email(securityUser.getUser().getEmail()).roles(roles).build();

			log.info("User authenticated successfully: {}", securityUser.getUsername());
			return ApiResponse.success("Authentication successful", authResponse);

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
			throw new BusinessException(BusinessCode.ERROR, "Authentication service error");
		}
	}

	@Override
	@LogOperation("Send Login OTP")
	public ApiResponse<Void> sendOtp(String email)
	{
		userRepository.findByEmail(email).orElseThrow(
				() -> new BusinessException(BusinessCode.USER_NOT_FOUND, "No account linked to this email"));

		var otp = RandomUtil.randomNumbers(6);
		var otpKey = CacheConstants.OTP_CODE + email;
		Assert.isTrue(redisUtil.set(otpKey, otp, 5, TimeUnit.MINUTES),
				() -> new BusinessException(BusinessCode.ERROR, "Failed to store OTP code"));

		Map<String, Object> variables = Dict.create().set("otp", otp).set("expireMin", 5);

		TemplateMailMessage mailMessage = TemplateMailMessage.builder().to(email).subject("Nexus Login OTP")
				.templateName("otp-login").variables(variables).type(TemplateMailMessage.MailType.TEMPLATE).build();

		try
		{
			rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY, mailMessage);
			log.info("OTP task dispatched to MQ for {}: {}", email, otp);
		}
		catch (Exception e)
		{
			redisUtil.delete(otpKey);
			log.error("Failed to dispatch OTP email task to MQ for: {}", email, e);
			throw new BusinessException(BusinessCode.ERROR, "Failed to dispatch email task");
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

		var storedOtp = redisUtil.get(otpKey, String.class).orElse(null);
		Assert.isTrue(StrUtil.equals(storedOtp, code),
				() -> new BusinessException(BusinessCode.INVALID_TOKEN, "Invalid or expired verification code"));

		var user = userRepository.findByEmail(email)
				.orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));

		Assert.isTrue(user.getStatus() == UserStatus.ACTIVE,
				() -> new BusinessException(BusinessCode.ACCOUNT_DISABLED, "Account status: " + user.getStatus()));

		var securityUser = new SecurityUser(user);
		var authentication = new UsernamePasswordAuthenticationToken(securityUser, null, securityUser.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		var accessToken = jwtUtils.generateAccessToken(securityUser);
		redisUtil.delete(otpKey);

		var roles = securityUser.getAuthorities().stream().map(ga -> ga.getAuthority()).collect(Collectors.toSet());

		var authResponse = AuthResponse.builder().accessToken(accessToken).username(user.getUsername())
				.email(user.getEmail()).roles(roles).build();

		log.info("User logged in via OTP authentication: {}", user.getUsername());
		return ApiResponse.success("Login successful", authResponse);
	}

	@Override
	public ApiResponse<User> getAuthenticatedUser()
	{
		var username = SecurityContextHolder.getContext().getAuthentication().getName();
		return userRepository.findByUsername(username).map(ApiResponse::success)
				.orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND, "Identity data unavailable"));
	}

	@Override
	public ApiResponse<Void> logout()
	{
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (ObjectUtil.isNotNull(authentication) && authentication.getCredentials() instanceof String token)
		{
			// Blacklist token in Redis until it naturally expires
			long remainingTime = jwtUtils.getAccessTokenExpiration();
			String blacklistKey = "nexus:jwt:blacklist:" + token;
			redisUtil.set(blacklistKey, "logout", remainingTime, TimeUnit.MILLISECONDS);
			log.info("Token blacklisted for user: {}", authentication.getName());
		}
		SecurityContextHolder.clearContext();
		return ApiResponse.success("Logged out successfully", null);
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
		var userRole = roleRepository.findByCode("ROLE_USER").orElseGet(() ->
		{
			log.warn("Default 'ROLE_USER' role missing; initializing fallback");
			var newRole = new Role();
			newRole.setName("Standard User");
			newRole.setCode("ROLE_USER");
			newRole.setDescription("Default role for registered members");
			return roleRepository.save(newRole);
		});
		user.setRoles(Collections.singleton(userRole));
	}
}