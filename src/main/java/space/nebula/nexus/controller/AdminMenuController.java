package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MenuRequest;
import space.nebula.nexus.payload.response.MenuResponse;
import space.nebula.nexus.service.IMenuService;

import java.util.List;

/**
 * Controller for administrative system menu management. Provides hierarchical
 * access to navigation and permission structures.
 */
@Tag(name = "Admin Menu Management", description = "Endpoints for managing the system menu tree and permissions")
@RestController
@RequestMapping("/api/v1/admin/menus")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMenuController {

	private final IMenuService menuService;

	@GetMapping("/tree")
	@Operation(summary = "Retrieve complete menu hierarchy", description = "Fetch the entire system menu structure as a nested tree.")
	public ApiResponse<List<MenuResponse>> retrieveMenuTree() {
		return menuService.retrieveFullMenuTree();
	}

	@PostMapping
	@Operation(summary = "Create menu item", description = "Add a new item to the system menu hierarchy.")
	public ApiResponse<MenuResponse> createMenu(@Valid @RequestBody MenuRequest request) {
		return menuService.createMenu(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update menu item", description = "Modify an existing menu item's details, parent, or permissions.")
	public ApiResponse<MenuResponse> updateMenu(@Parameter(description = "Menu ID") @PathVariable Long id,
			@Valid @RequestBody MenuRequest request) {
		return menuService.updateMenu(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete menu item", description = "Permanently remove a menu item and its descendants.")
	public ApiResponse<Void> deleteMenu(@Parameter(description = "Menu ID") @PathVariable Long id) {
		return menuService.deleteMenu(id);
	}

	@GetMapping("/current")
	@Operation(summary = "Retrieve current user menu", description = "Fetch the menu hierarchy filtered by the authenticated user's permissions.")
	public ApiResponse<List<MenuResponse>> retrieveAuthenticatedUserMenus() {
		return menuService.retrieveAuthenticatedUserMenuTree();
	}
}
