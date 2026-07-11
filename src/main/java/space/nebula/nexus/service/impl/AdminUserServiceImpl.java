package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.mapper.UserMapper;
import space.nebula.nexus.payload.request.AssignRoleRequest;
import space.nebula.nexus.payload.response.UserResponse;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.service.IAdminUserService;

import cn.hutool.core.lang.Assert;

import java.util.HashSet;
import java.util.List;

/**
 * Implementation of administrative user management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService
{
	private static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserMapper userMapper;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<UserResponse>> getAllUsers()
	{
		var users = userRepository.findAll();
		return ApiResponse.success(userMapper.toResponseList(users));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<UserResponse> getUserById(Long id)
	{
		var user = userRepository.findById(id).orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));
		return ApiResponse.success(userMapper.toResponse(user));
	}

	@Override
	@Transactional
	@LogOperation("Disable User")
	public ApiResponse<Void> disableUser(Long id)
	{
		var user = userRepository.findById(id).orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));

		Assert.isFalse(user.getStatus() == UserStatus.INACTIVE,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "User is already inactive"));
		protectLastActiveAdministrator(user, false);

		user.setStatus(UserStatus.INACTIVE);
		userRepository.save(user);
		log.info("Admin disabled user id: {}", id);
		return ApiResponse.success("User disabled successfully", null);
	}

	@Override
	@Transactional
	@LogOperation("Enable User")
	public ApiResponse<Void> enableUser(Long id)
	{
		var user = userRepository.findById(id).orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));

		Assert.isFalse(user.getStatus() == UserStatus.ACTIVE,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "User is already active"));

		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
		log.info("Admin enabled user id: {}", id);
		return ApiResponse.success("User enabled successfully", null);
	}

	@Override
	@Transactional
	@LogOperation("Delete User")
	public ApiResponse<Void> deleteUser(Long id)
	{
		var user = userRepository.findById(id)
				.orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));
		protectLastActiveAdministrator(user, false);
		userRepository.delete(user);
		log.info("Admin deleted user id: {}", id);
		return ApiResponse.success("User deleted successfully", null);
	}

	@Override
	@Transactional
	@LogOperation("Audit User Registration")
	public ApiResponse<Void> auditUser(Long id, boolean approved)
	{
		var user = userRepository.findById(id).orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));

		Assert.isTrue(user.getStatus() == UserStatus.PENDING,
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only pending registrations can be audited"));

		if (approved)
		{
			user.setStatus(UserStatus.ACTIVE);
			log.info("Admin approved registration for user: {}", user.getUsername());
		}
		else
		{
			user.setStatus(UserStatus.INACTIVE);
			log.info("Admin rejected registration for user: {}", user.getUsername());
		}

		userRepository.save(user);
		return ApiResponse.success(approved ? "User registration approved" : "User registration rejected", null);
	}

	@Override
	@Transactional
	@LogOperation("Assign Roles to User")
	public ApiResponse<Void> assignRoles(Long userId, AssignRoleRequest request)
	{
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(BusinessCode.USER_NOT_FOUND));

		var roles = roleRepository.findAllById(request.roleIds());
		Assert.notEmpty(roles, () -> new BusinessException(BusinessCode.BAD_REQUEST, "No valid roles found for provided IDs"));
		Assert.isTrue(roles.size() == request.roleIds().size(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Some role IDs provided do not exist"));
		boolean retainsAdminRole = roles.stream().anyMatch(role -> ADMIN_ROLE_CODE.equals(role.getCode()));
		protectLastActiveAdministrator(user, retainsAdminRole);

		user.setRoles(new HashSet<>(roles));
		userRepository.save(user);

		log.info("Assigned roles {} to user id: {}", request.roleIds(), userId);
		return ApiResponse.success("User roles updated successfully", null);
	}

	private void protectLastActiveAdministrator(space.nebula.nexus.entity.User target, boolean retainsAdminRole)
	{
		boolean removesActiveAdmin = target.getStatus() == UserStatus.ACTIVE
				&& target.getRoles().stream().anyMatch(role -> ADMIN_ROLE_CODE.equals(role.getCode()))
				&& !retainsAdminRole;
		if (!removesActiveAdmin)
		{
			return;
		}

		List<space.nebula.nexus.entity.User> activeAdmins =
				userRepository.findByRoleCodeAndStatusForUpdate(ADMIN_ROLE_CODE, UserStatus.ACTIVE);
		Assert.isTrue(activeAdmins.stream().anyMatch(user -> !user.getId().equals(target.getId())),
				() -> new BusinessException(BusinessCode.BAD_REQUEST,
						"At least one active administrator must remain"));
	}
}
