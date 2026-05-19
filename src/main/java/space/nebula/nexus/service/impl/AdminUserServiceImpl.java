package space.nebula.nexus.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.mapper.UserMapper;
import space.nebula.nexus.payload.response.UserResponse;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IAdminUserService;

import java.util.List;

@Slf4j
@Service
public class AdminUserServiceImpl implements IAdminUserService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private UserMapper userMapper;

    @Override
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userMapper.toResponseList(userRepository.findAll()));
    }

    @Override
    public ApiResponse<UserResponse> getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));
        return ApiResponse.success(userMapper.toResponse(user));
    }

    @Override
    @Transactional
    @LogOperation("Disable User")
    public ApiResponse<Void> disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));
        
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(BusinessCode.BAD_REQUEST, "User is already inactive");
        }
        
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        log.info("Admin disabled user id: {}", id);
        return ApiResponse.success("User disabled successfully", null);
    }

    @Override
    @Transactional
    @LogOperation("Enable User")
    public ApiResponse<Void> enableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));
        
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BusinessException(BusinessCode.BAD_REQUEST, "User is already active");
        }
        
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        log.info("Admin enabled user id: {}", id);
        return ApiResponse.success("User enabled successfully", null);
    }

    @Override
    @Transactional
    @LogOperation("Delete User")
    public ApiResponse<Void> deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new BusinessException(BusinessCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(id);
        log.info("Admin deleted user id: {}", id);
        return ApiResponse.success("User deleted successfully", null);
    }
}
