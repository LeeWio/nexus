package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.mapper.RoleMapper;
import space.nebula.nexus.payload.request.AssignMenuRequest;
import space.nebula.nexus.payload.request.RoleRequest;
import space.nebula.nexus.payload.response.RoleResponse;
import space.nebula.nexus.repository.MenuRepository;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.service.IAdminRoleService;

import java.util.HashSet;
import java.util.List;

/**
 * Implementation of administrative role management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements IAdminRoleService
{

	private final RoleRepository roleRepository;
	private final MenuRepository menuRepository;
	private final RoleMapper roleMapper;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<RoleResponse>> getAllRoles()
	{
		return ApiResponse.success(roleMapper.toResponseList(roleRepository.findAll()));
	}

	@Override
	@Transactional
	@LogOperation("Create Role")
	public ApiResponse<RoleResponse> createRole(RoleRequest request)
	{
		Assert.isFalse(roleRepository.findByCode(request.code()).isPresent(),
				() -> new BusinessException(BusinessCode.DUPLICATE_KEY, "Role code already exists: " + request.code()));

		var role = new Role();
		role.setName(request.name());
		role.setCode(request.code());
		role.setDescription(request.description());

		var savedRole = roleRepository.save(role);
		log.info("Admin created new role: {}", savedRole.getCode());
		return ApiResponse.success("Role created successfully", roleMapper.toResponse(savedRole));
	}

	@Override
	@Transactional
	@LogOperation("Update Role")
	public ApiResponse<RoleResponse> updateRole(Long id, RoleRequest request)
	{
		var role = roleRepository.findById(id)
				.orElseThrow(() -> new BusinessException(BusinessCode.NOT_FOUND, "Role not found"));

		if (ObjectUtil.notEqual(role.getCode(), request.code()))
		{
			Assert.isFalse(roleRepository.findByCode(request.code()).isPresent(),
					() -> new BusinessException(BusinessCode.DUPLICATE_KEY,
							"Role code already exists: " + request.code()));
		}

		role.setName(request.name());
		role.setCode(request.code());
		role.setDescription(request.description());

		roleRepository.save(role);
		log.info("Admin updated role id: {}", id);
		return ApiResponse.success("Role updated successfully", roleMapper.toResponse(role));
	}

	@Override
	@Transactional
	@LogOperation("Delete Role")
	public ApiResponse<Void> deleteRole(Long id)
	{
		Assert.isTrue(roleRepository.existsById(id),
				() -> new BusinessException(BusinessCode.NOT_FOUND, "Role not found"));

		roleRepository.deleteById(id);
		log.info("Admin deleted role id: {}", id);
		return ApiResponse.success("Role deleted successfully", null);
	}

	@Override
	@Transactional
	@LogOperation("Assign Menus to Role")
	public ApiResponse<Void> assignMenus(Long roleId, AssignMenuRequest request)
	{
		var role = roleRepository.findById(roleId)
				.orElseThrow(() -> new BusinessException(BusinessCode.NOT_FOUND, "Role not found"));

		var menus = menuRepository.findAllById(request.menuIds());
		Assert.isTrue(menus.size() == request.menuIds().size(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "One or more menu IDs are invalid"));

		role.setMenus(new HashSet<>(menus));
		roleRepository.save(role);

		log.info("Assigned menus {} to role id: {}", request.menuIds(), roleId);
		return ApiResponse.success("Menus assigned successfully", null);
	}
}
