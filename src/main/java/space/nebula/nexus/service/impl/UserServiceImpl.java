package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.mapper.UserMapper;
import space.nebula.nexus.payload.request.PasswordChangeRequest;
import space.nebula.nexus.payload.request.UserProfileRequest;
import space.nebula.nexus.payload.response.UserInfoResponse;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IAuthService;
import space.nebula.nexus.service.IUserService;

/**
 * Implementation of user self-service operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final IAuthService authService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<UserInfoResponse> getCurrentUserInfo() {
        User currentUser = authService.getAuthenticatedUser().data();
        return ApiResponse.success(userMapper.toInfoResponse(currentUser));
    }

    @Override
    @Transactional
    @LogOperation("Update Profile")
    public ApiResponse<Void> updateProfile(UserProfileRequest request) {
        User currentUser = authService.getAuthenticatedUser().data();
        
        userMapper.updateEntity(currentUser, request);
        userRepository.save(currentUser);
        
        log.info("User {} updated profile", currentUser.getUsername());
        return ApiResponse.success("Profile updated successfully", null);
    }

    @Override
    @Transactional
    @LogOperation("Change Password")
    public ApiResponse<Void> changePassword(PasswordChangeRequest request) {
        User currentUser = authService.getAuthenticatedUser().data();
        
        Assert.isTrue(passwordEncoder.matches(request.currentPassword(), currentUser.getPassword()),
            () -> new BusinessException(BusinessCode.BAD_CREDENTIALS, "Current password does not match"));
            
        currentUser.setPassword(passwordEncoder.encode(request.newPassword()));
		currentUser.setTokenVersion(currentUser.getTokenVersion() + 1);
        userRepository.save(currentUser);
        
        log.info("User {} changed password", currentUser.getUsername());
        return ApiResponse.success("Password changed successfully", null);
    }
}
