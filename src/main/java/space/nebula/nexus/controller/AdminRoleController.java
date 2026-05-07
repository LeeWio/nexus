package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.RoleRequest;
import space.nebula.nexus.payload.response.RoleResponse;
import space.nebula.nexus.service.IAdminRoleService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/roles")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Role Management", description = "Endpoints for managing roles (Requires ADMIN role)")
public class AdminRoleController {

    @Resource
    private IAdminRoleService adminRoleService;

    @GetMapping
    @Operation(summary = "Get all roles")
    public ApiResponse<List<RoleResponse>> getAllRoles() {
        return adminRoleService.getAllRoles();
    }

    @PostMapping
    @Operation(summary = "Create a new role")
    public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleRequest request) {
        return adminRoleService.createRole(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing role")
    public ApiResponse<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return adminRoleService.updateRole(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role")
    public ApiResponse<Void> deleteRole(@PathVariable Long id) {
        return adminRoleService.deleteRole(id);
    }
}
