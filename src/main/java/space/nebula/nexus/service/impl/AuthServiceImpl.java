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
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.validator.UserValidator;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.service.LoginSecurityService;
import space.nebula.nexus.security.util.JwtUtils;
import space.nebula.nexus.service.IAuthService;

import java.util.Collections;
import java.util.Set;
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

    @Override
    @Transactional
    public ApiResponse<Void> register(RegisterRequest request) {
        userValidator.validateRegistration(request);

        User user = createNewUser(request);
        assignDefaultRole(user);

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());
        return ApiResponse.success("User registered successfully", null);
    }

    @Override
    public ApiResponse<AuthResponse> login(LoginRequest request) {
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
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            String token = jwtUtils.generateAccessToken(userDetails);
            
            Set<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(token)
                    .username(userDetails.getUsername())
                    .roles(roles)
                    .build();

            log.info("User logged in: {}", userDetails.getUsername());
            return ApiResponse.success("Login successful", authResponse);

        } catch (BadCredentialsException e) {
            // 3. Delegate failure recording
            loginSecurityService.recordLoginFailure(username);
            throw new BusinessException(401, "Invalid username or password");
        } catch (BusinessException e) {
            throw e; // Rethrow business exceptions (like lockouts)
        } catch (Exception e) {
            log.error("Login unexpected error for user: {}", username, e);
            throw new BusinessException(500, "Login failed: " + e.getMessage());
        }
    }

    @Override
    public ApiResponse<User> getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .map(ApiResponse::success)
                .orElseThrow(() -> new BusinessException(404, "Current user not found"));
    }

    // --- Private Helpers ---

    private User createNewUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
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
