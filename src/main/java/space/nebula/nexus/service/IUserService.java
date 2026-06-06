package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PasswordChangeRequest;
import space.nebula.nexus.payload.request.UserProfileRequest;
import space.nebula.nexus.payload.response.UserInfoResponse;

/**
 * Service for user self-service operations (profile, password).
 */
public interface IUserService {

    /**
     * Get current user info.
     */
    ApiResponse<UserInfoResponse> getCurrentUserInfo();

    /**
     * Update current user profile.
     */
    ApiResponse<Void> updateProfile(UserProfileRequest request);

    /**
     * Change current user password.
     */
    ApiResponse<Void> changePassword(PasswordChangeRequest request);
}
