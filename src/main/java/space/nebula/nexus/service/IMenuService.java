package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MenuRequest;
import space.nebula.nexus.payload.response.MenuResponse;

import java.util.List;

public interface IMenuService {
    ApiResponse<List<MenuResponse>> getMenuTree();
    ApiResponse<MenuResponse> createMenu(MenuRequest request);
    ApiResponse<MenuResponse> updateMenu(Long id, MenuRequest request);
    ApiResponse<Void> deleteMenu(Long id);
    ApiResponse<List<MenuResponse>> getCurrentUserMenuTree();
}
