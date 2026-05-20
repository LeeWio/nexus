package space.nebula.nexus.service.impl;

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
import space.nebula.nexus.payload.request.RoleRequest;
import space.nebula.nexus.payload.response.RoleResponse;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.service.IAdminRoleService;

import java.util.List;

/**
 * Implementation of administrative role management service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRoleServiceImpl implements IAdminRoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        var roles = roleRepository.findAll();
        return ApiResponse.success(roleMapper.toResponseList(roles));
    }

    @Override
    @Transactional
    @LogOperation("Create Role")
    public ApiResponse<RoleResponse> createRole(RoleRequest request) {
        if (roleRepository.findByCode(request.code()).isPresent()) {
            throw new BusinessException(BusinessCode.DUPLICATE_KEY, "Role code already exists");
        }
        
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
    public ApiResponse<RoleResponse> updateRole(Long id, RoleRequest request) {
        var role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(BusinessCode.NOT_FOUND, "Role not found"));

        if (!role.getCode().equals(request.code()) && roleRepository.findByCode(request.code()).isPresent()) {
            throw new BusinessException(BusinessCode.DUPLICATE_KEY, "Role code already exists");
        }
        
        role.setName(request.name());
        role.setCode(request.code());
        role.setDescription(request.description());
        
        var updatedRole = roleRepository.save(role);
        log.info("Admin updated role id: {}", id);
        return ApiResponse.success("Role updated successfully", roleMapper.toResponse(updatedRole));
    }

    @Override
    @Transactional
    @LogOperation("Delete Role")
    public ApiResponse<Void> deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new BusinessException(BusinessCode.NOT_FOUND, "Role not found");
        }
        roleRepository.deleteById(id);
        log.info("Admin deleted role id: {}", id);
        return ApiResponse.success("Role deleted successfully", null);
    }
}
