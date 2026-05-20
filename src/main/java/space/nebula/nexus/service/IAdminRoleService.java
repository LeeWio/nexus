package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.AssignMenuRequest;
import space.nebula.nexus.payload.request.RoleRequest;
import space.nebula.nexus.payload.response.RoleResponse;

import java.util.List;

public interface IAdminRoleService {

	ApiResponse<List<RoleResponse>> getAllRoles();

	ApiResponse<RoleResponse> createRole(RoleRequest request);

	ApiResponse<RoleResponse> updateRole(Long id, RoleRequest request);

	ApiResponse<Void> deleteRole(Long id);

	ApiResponse<Void> assignMenus(Long roleId, AssignMenuRequest request);
}
