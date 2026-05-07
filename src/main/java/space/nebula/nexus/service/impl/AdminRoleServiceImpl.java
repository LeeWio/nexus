package space.nebula.nexus.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.Role;
import space.nebula.nexus.mapper.RoleMapper;
import space.nebula.nexus.payload.request.RoleRequest;
import space.nebula.nexus.payload.response.RoleResponse;
import space.nebula.nexus.repository.RoleRepository;
import space.nebula.nexus.service.IAdminRoleService;

import java.util.List;

@Slf4j
@Service
public class AdminRoleServiceImpl implements IAdminRoleService {

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private RoleMapper roleMapper;

    @Override
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return ApiResponse.success(roleMapper.toResponseList(roleRepository.findAll()));
    }

    @Override
    @Transactional
    @LogOperation("Create Role")
    public ApiResponse<RoleResponse> createRole(RoleRequest request) {
        if (roleRepository.findByCode(request.code()).isPresent()) {
            throw new BusinessException("Role code already exists");
        }
        
        Role role = new Role();
        role.setName(request.name());
        role.setCode(request.code());
        role.setDescription(request.description());
        
        roleRepository.save(role);
        log.info("Admin created new role: {}", role.getCode());
        return ApiResponse.success("Role created successfully", roleMapper.toResponse(role));
    }

    @Override
    @Transactional
    @LogOperation("Update Role")
    public ApiResponse<RoleResponse> updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Role not found"));

        if (!role.getCode().equals(request.code()) && roleRepository.findByCode(request.code()).isPresent()) {
            throw new BusinessException("Role code already exists");
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
    public ApiResponse<Void> deleteRole(Long id) {
        if (!roleRepository.existsById(id)) {
            throw new BusinessException(404, "Role not found");
        }
        roleRepository.deleteById(id);
        log.info("Admin deleted role id: {}", id);
        return ApiResponse.success("Role deleted successfully", null);
    }
}
