package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.mapper.UserMapper;
import space.nebula.nexus.payload.request.PasswordChangeRequest;
import space.nebula.nexus.payload.request.UserProfileRequest;
import space.nebula.nexus.payload.response.UserInfoResponse;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IAuthService;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private IAuthService authService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("encodedOldPassword");
        testUser.setRoles(Collections.emptySet());
    }

    @Test
    void getCurrentUserInfo_Success() {
        when(authService.getAuthenticatedUser()).thenReturn(ApiResponse.success(testUser));
        when(userMapper.toInfoResponse(testUser)).thenReturn(new UserInfoResponse(1L, "testuser", null, null, Collections.emptySet(), Collections.emptySet()));

        var response = userService.getCurrentUserInfo();

        assertNotNull(response);
        assertEquals("testuser", response.data().username());
    }

    @Test
    void updateProfile_Success() {
        UserProfileRequest request = new UserProfileRequest("NewNick", null, "New bio", null, null, null);
        when(authService.getAuthenticatedUser()).thenReturn(ApiResponse.success(testUser));

        var response = userService.updateProfile(request);

        assertEquals(200, response.code());
        verify(userMapper).updateEntity(testUser, request);
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_Success() {
        PasswordChangeRequest request = new PasswordChangeRequest("oldPassword", "newPassword");
        when(authService.getAuthenticatedUser()).thenReturn(ApiResponse.success(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        var response = userService.changePassword(request);

        assertEquals(200, response.code());
        assertEquals("encodedNewPassword", testUser.getPassword());
        verify(userRepository).save(testUser);
    }
}
