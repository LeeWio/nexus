package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.validator.UserValidator;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.payload.request.LoginRequest;
import space.nebula.nexus.payload.request.RegisterRequest;
import space.nebula.nexus.payload.response.AuthResponse;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.security.service.LoginSecurityService;
import space.nebula.nexus.security.util.JwtUtils;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
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
        assertEquals(200, response.getCode());
        verify(userValidator).validateRegistration(registerRequest);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should authenticate successfully and reset failure count")
    void authenticate_Success() {
        // Arrange
        Authentication auth = mock(Authentication.class);
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateAccessToken(any())).thenReturn("mock-token");

        // Act
        ApiResponse<AuthResponse> response = authService.authenticate(loginRequest);

        // Assert
        assertEquals(200, response.getCode());
        assertEquals("mock-token", response.getData().accessToken());
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
        assertEquals(401, exception.getCode());
        verify(loginSecurityService).recordLoginFailure("testuser");
    }

    @Test
    @DisplayName("Should fail authentication if account is locked")
    void authenticate_AccountLocked() {
        // Arrange
        doThrow(new BusinessException(403, "Account locked"))
                .when(loginSecurityService).validateLoginLock("testuser");

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> authService.authenticate(loginRequest));
        assertEquals(403, exception.getCode());
    }
}
