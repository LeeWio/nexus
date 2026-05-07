package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.UserResponse;
import space.nebula.nexus.service.IAdminUserService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin User Management", description = "Endpoints for managing users (Requires ADMIN role)")
public class AdminUserController {

    @Resource
    private IAdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Get all users")
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return adminUserService.getAllUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a user by ID")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return adminUserService.getUserById(id);
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "Disable a user account")
    public ApiResponse<Void> disableUser(@PathVariable Long id) {
        return adminUserService.disableUser(id);
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "Enable a user account")
    public ApiResponse<Void> enableUser(@PathVariable Long id) {
        return adminUserService.enableUser(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user account")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        return adminUserService.deleteUser(id);
    }
}
