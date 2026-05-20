package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.AssignRoleRequest;
import space.nebula.nexus.payload.response.UserResponse;

import java.util.List;

public interface IAdminUserService {

	ApiResponse<List<UserResponse>> getAllUsers();

	ApiResponse<UserResponse> getUserById(Long id);

	ApiResponse<Void> disableUser(Long id);

	ApiResponse<Void> enableUser(Long id);

	ApiResponse<Void> deleteUser(Long id);

	ApiResponse<Void> assignRoles(Long userId, AssignRoleRequest request);
}
