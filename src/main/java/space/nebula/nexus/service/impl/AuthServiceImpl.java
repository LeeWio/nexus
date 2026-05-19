package space.nebula.nexus.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.validator.UserValidator;
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
import space.nebula.nexus.security.util.JwtUtils;
import space.nebula.nexus.service.IAuthService;
import space.nebula.nexus.utils.MailUtil;
import space.nebula.nexus.utils.RedisUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Professional implementation of IAuthService.
 * Refactored to delegate security state management to LoginSecurityService.
 */
@Slf4j
@Service
public class AuthServiceImpl implements IAuthService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private JwtUtils jwtUtils;

    @Resource
    private UserValidator userValidator;

    @Resource
    private LoginSecurityService loginSecurityService;

    @Resource
    private RedisUtil redisUtil;

    @Resource
    private MailUtil mailUtil;

    @Override
    @Transactional
    @LogOperation("User Registration")
    public ApiResponse<Void> registerAccount(RegisterRequest request) {
        userValidator.validateRegistration(request);

        User newUser = createNewUser(request);
        assignDefaultRole(newUser);

        userRepository.save(newUser);
        log.info("User account registered successfully, pending audit: {}", newUser.getUsername());
        return ApiResponse.success("Registration successful. Your account is currently pending administrator approval.", null);
    }

    @Override
    @LogOperation("User Login")
    public ApiResponse<AuthResponse> authenticate(LoginRequest request) {
        String username = request.username();

        // 1. Delegate lock check to specialized service
        loginSecurityService.validateLoginLock(username);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password())
            );

            // 2. Success: reset failure count
            loginSecurityService.resetLoginFailure(username);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();
            
            String accessToken = jwtUtils.generateAccessToken(securityUser);
            
            Set<String> userRoles = securityUser.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(accessToken)
                    .username(securityUser.getUsername())
                    .email(securityUser.getUser().getEmail())
                    .roles(userRoles)
                    .build();

            log.info("User authenticated successfully: {}", securityUser.getUsername());
            return ApiResponse.success("Authentication successful", authResponse);

        } catch (BadCredentialsException e) {
            // 3. Delegate failure recording
            loginSecurityService.recordLoginFailure(username);
            throw new BusinessException(BusinessCode.BAD_CREDENTIALS);
        } catch (BusinessException e) {
            throw e; // Rethrow business exceptions (like lockouts)
        } catch (Exception e) {
            log.error("Authentication failed unexpectedly for user: {}", username, e);
            throw new BusinessException(BusinessCode.ERROR, "Authentication failed: " + e.getMessage());
        }
    }

    @Override
    @LogOperation("Send Login OTP")
    public ApiResponse<Void> sendOtp(String email) {
        // 1. Check if user exists by email
        userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND, "No user found with this email"));

        // 2. Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));

        // 3. Save to Redis with 5 min expiration
        String otpKey = CacheConstants.OTP_CODE + email;
        redisUtil.set(otpKey, otp, 5, TimeUnit.MINUTES);

        // 4. Send email
        Map<String, Object> variables = new HashMap<>();
        variables.put("otp", otp);
        variables.put("expireMin", 5);
        mailUtil.sendTemplateMail(email, "Nexus Login OTP", "otp-login", variables);

        log.info("OTP sent to email: {}", email);
        return ApiResponse.success("OTP sent successfully. Please check your email.", null);
    }

    @Override
    @Transactional
    @LogOperation("OTP Login")
    public ApiResponse<AuthResponse> loginWithOtp(OtpLoginRequest request) {
        String email = request.email();
        String code = request.code();

        // 1. Verify OTP from Redis
        String otpKey = CacheConstants.OTP_CODE + email;
        String storedOtp = redisUtil.get(otpKey, String.class).orElse(null);

        if (storedOtp == null || !storedOtp.equals(code)) {
            throw new BusinessException(BusinessCode.INVALID_TOKEN, "Invalid or expired OTP");
        }

        // 2. Load user and validate status
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(BusinessCode.ACCOUNT_DISABLED, "Your account is " + user.getStatus().name() + ". Please contact the administrator.");
        }

        // 3. Authenticate manually in SecurityContext
        SecurityUser securityUser = new SecurityUser(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                securityUser, null, securityUser.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 4. Generate Token and Response
        String accessToken = jwtUtils.generateAccessToken(securityUser);
        redisUtil.delete(otpKey); // Consume OTP

        Set<String> userRoles = securityUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(userRoles)
                .build();

        log.info("User logged in via OTP: {}", user.getUsername());
        return ApiResponse.success("Login successful", authResponse);
    }

    @Override
    public ApiResponse<User> getAuthenticatedUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(ApiResponse::success)
                .orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND, "Authenticated user data not found"));
    }

    // --- Private Helpers ---

    private User createNewUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING);
        return user;
    }

    private void assignDefaultRole(User user) {
        Role userRole = roleRepository.findByCode("ROLE_USER")
                .orElseGet(() -> {
                    log.warn("Default role 'ROLE_USER' missing, creating on the fly");
                    Role newRole = new Role();
                    newRole.setName("Standard User");
                    newRole.setCode("ROLE_USER");
                    newRole.setDescription("Default role for registered users");
                    return roleRepository.save(newRole);
                });
        user.setRoles(Collections.singleton(userRole));
    }
}
