package space.nebula.nexus.service;

import cn.hutool.core.lang.tree.Tree;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MenuRequest;
import space.nebula.nexus.payload.response.MenuResponse;

import java.util.List;

public interface IMenuService {

	/**
	 * Retrieves the entire menu tree for administration.
	 */
	ApiResponse<List<Tree<Long>>> retrieveFullMenuTree();

	ApiResponse<MenuResponse> createMenu(MenuRequest request);

	ApiResponse<MenuResponse> updateMenu(Long id, MenuRequest request);

	ApiResponse<Void> deleteMenu(Long id);

	/**
	 * Retrieves the menu tree tailored for the currently authenticated user's
	 * permissions.
	 */
	ApiResponse<List<Tree<Long>>> retrieveAuthenticatedUserMenuTree();

	/**
	 * Retrieves the public navigation menu tree for the website frontend.
	 */
	ApiResponse<List<Tree<Long>>> retrievePublicNavigationMenuTree();
}
