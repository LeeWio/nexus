package space.nebula.nexus.service;

import java.util.List;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.PasswordChangeRequest;
import space.nebula.nexus.payload.request.UserProfileRequest;
import space.nebula.nexus.payload.response.UserInfoResponse;
import space.nebula.nexus.payload.response.UserMentionResponse;

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

	/**
	 * Search active users for @mention pickers (authenticated callers only).
	 */
	ApiResponse<List<UserMentionResponse>> searchMentionableUsers(String query, int limit);
}
