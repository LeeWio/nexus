package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PasswordChangeRequest;
import space.nebula.nexus.payload.request.UserProfileRequest;
import space.nebula.nexus.payload.response.UserInfoResponse;
import space.nebula.nexus.service.IUserService;

/**
 * Controller for user self-service operations like profile management and security.
 */
@Tag(name = "User Profile", description = "Endpoints for users to manage their own profile and security settings")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user info", description = "Returns profile details and permissions for the currently authenticated user.")
	public ApiResponse<UserInfoResponse> getCurrentUser() {
        return userService.getCurrentUserInfo();
    }

    @PutMapping("/profile")
    @Operation(summary = "Update profile", description = "Updates the current user's profile information like nickname, avatar, and bio.")
    public ApiResponse<Void> updateProfile(@Valid @RequestBody UserProfileRequest request) {
        return userService.updateProfile(request);
    }

    @PutMapping("/password")
    @Operation(summary = "Change password", description = "Updates the current user's password. Requires the old password for verification.")
    public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        return userService.changePassword(request);
    }
}
