package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.AssignMenuRequest;
import space.nebula.nexus.payload.request.RoleRequest;
import space.nebula.nexus.payload.response.RoleResponse;
import space.nebula.nexus.service.IAdminRoleService;

import java.util.List;

/**
 * Controller for administrative role management. Provides endpoints for
 * managing system roles and their permissions.
 */
@Tag(name = "Admin Role Management", description = "Endpoints for managing system roles and security definitions")
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoleController
{

	private final IAdminRoleService adminRoleService;

	@GetMapping
	@Operation(summary = "Get all roles", description = "Retrieve a list of all security roles defined in the system.")
	public ApiResponse<List<RoleResponse>> getAllRoles()
	{
		return adminRoleService.getAllRoles();
	}

	@PostMapping
	@Operation(summary = "Create role", description = "Define a new security role with a unique code and description.")
	public ApiResponse<RoleResponse> createRole(@Valid @RequestBody RoleRequest request)
	{
		return adminRoleService.createRole(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update role", description = "Modify an existing role's name, code, or description.")
	public ApiResponse<RoleResponse> updateRole(@Parameter(description = "Role ID") @PathVariable Long id,
			@Valid @RequestBody RoleRequest request)
	{
		return adminRoleService.updateRole(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete role", description = "Permanently remove a security role from the system.")
	public ApiResponse<Void> deleteRole(@Parameter(description = "Role ID") @PathVariable Long id)
	{
		return adminRoleService.deleteRole(id);
	}

	@PostMapping("/{id}/menus")
	@Operation(summary = "Assign menus to role", description = "Link a set of menus or navigation items to a specific role.")
	public ApiResponse<Void> assignMenus(@Parameter(description = "Role ID") @PathVariable Long id,
			@Valid @RequestBody AssignMenuRequest request)
	{
		return adminRoleService.assignMenus(id, request);
	}

	@GetMapping("/{id}/menus")
	@Operation(summary = "Get role menus", description = "Retrieve a list of menu IDs assigned to a specific role.")
	public ApiResponse<List<Long>> getRoleMenus(@Parameter(description = "Role ID") @PathVariable Long id)
	{
		return adminRoleService.getRoleMenuIds(id);
	}
}
