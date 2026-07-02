package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.payload.request.AssignRoleRequest;
import space.nebula.nexus.payload.response.UserResponse;
import space.nebula.nexus.service.IAdminUserService;

import java.util.List;

/**
 * Controller for administrative user account management. Provides endpoints for
 * auditing, enabling/disabling, and managing user lifecycle.
 */
@Tag(name = "Admin User Management", description = "Endpoints for managing system users and account security")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController
{

	private final IAdminUserService adminUserService;

	@GetMapping
	@Operation(summary = "Get all users", description = "Retrieve a comprehensive list of all registered users.")
	public ApiResponse<List<UserResponse>> getAllUsers()
	{
		return adminUserService.getAllUsers();
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get user by ID", description = "Fetch detailed profile and status for a specific user.")
	public ApiResponse<UserResponse> getUserById(@Parameter(description = "User ID") @PathVariable Long id)
	{
		return adminUserService.getUserById(id);
	}

	@PatchMapping("/{id}/status")
	@Operation(summary = "Update user status", description = "Approve, suspend, or activate a user account.")
	public ApiResponse<Void> updateUserStatus(
			@Parameter(description = "User ID") @PathVariable Long id,
			@Parameter(description = "Target status (e.g., ACTIVE, INACTIVE)") @RequestParam UserStatus status)
	{
		if (status == UserStatus.ACTIVE)
		{
			return adminUserService.enableUser(id);
		}
		else if (status == UserStatus.INACTIVE)
		{
			return adminUserService.disableUser(id);
		}
		else
		{
			throw new IllegalArgumentException("Unsupported user status update: " + status);
		}
	}

	@PutMapping("/{id}/disable")
	@Operation(summary = "Disable user", description = "Suspend a user account to prevent login and access.")
	public ApiResponse<Void> disableUser(@Parameter(description = "User ID") @PathVariable Long id)
	{
		return adminUserService.disableUser(id);
	}

	@PutMapping("/{id}/enable")
	@Operation(summary = "Enable user", description = "Restore access for a previously disabled user account.")
	public ApiResponse<Void> enableUser(@Parameter(description = "User ID") @PathVariable Long id)
	{
		return adminUserService.enableUser(id);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete user", description = "Permanently remove a user account from the system.")
	public ApiResponse<Void> deleteUser(@Parameter(description = "User ID") @PathVariable Long id)
	{
		return adminUserService.deleteUser(id);
	}

	@PostMapping("/{id}/audit")
	@Operation(summary = "Audit user registration", description = "Approve or reject a pending user registration request.")
	public ApiResponse<Void> auditUser(@Parameter(description = "User ID") @PathVariable Long id,
			@Parameter(description = "Approval status") @RequestParam boolean approved)
	{
		return adminUserService.auditUser(id, approved);
	}

	@RequestMapping(value = "/{id}/roles", method = {RequestMethod.POST, RequestMethod.PUT})
	@Operation(summary = "Assign roles to user", description = "Associate a set of security roles with a specific user (supports both POST and PUT).")
	public ApiResponse<Void> assignRoles(@Parameter(description = "User ID") @PathVariable Long id,
			@Valid @RequestBody AssignRoleRequest request)
	{
		return adminUserService.assignRoles(id, request);
	}
}
