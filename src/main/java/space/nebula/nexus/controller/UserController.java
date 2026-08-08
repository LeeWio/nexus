package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
 * Controller for user self-service operations like profile management and
 * security.
 */
@Tag(name = "User Profile", description = "Endpoints for users to manage their own profile and security settings")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

	private final IUserService userService;

	@GetMapping("/me")
	@Operation(summary = "Get current user info", description = "Returns the profile, role codes, and permission codes for the JWT-authenticated user. Use permissions for frontend feature gating, not as a substitute for backend authorization.")
	public ApiResponse<UserInfoResponse> getCurrentUser() {
		return userService.getCurrentUserInfo();
	}

	@PutMapping("/profile")
	@Operation(summary = "Update current user profile", description = "Updates only supplied profile fields for the authenticated user. Password and role changes use separate endpoints and cannot be changed here.")
	public ApiResponse<Void> updateProfile(@Valid @RequestBody UserProfileRequest request) {
		return userService.updateProfile(request);
	}

	@PutMapping("/password")
	@Operation(summary = "Change current user password", description = "Updates the authenticated user's password after verifying currentPassword. Existing access tokens may be invalidated according to account security policy.")
	public ApiResponse<Void> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
		return userService.changePassword(request);
	}
}
