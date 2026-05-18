package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/admin/menus")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Menu Management", description = "Endpoints for managing the system menu and public navigation")
public class AdminMenuController {

    private final IMenuService menuService;

    @GetMapping("/tree")
    @Operation(summary = "Retrieve the complete menu hierarchy")
    public ApiResponse<List<MenuResponse>> retrieveMenuTree() {
        return menuService.retrieveFullMenuTree();
    }

    @PostMapping
    @Operation(summary = "Create a new menu item")
    public ApiResponse<MenuResponse> createMenu(@Valid @RequestBody MenuRequest request) {
        return menuService.createMenu(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing menu item")
    public ApiResponse<MenuResponse> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuRequest request) {
        return menuService.updateMenu(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a menu item")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        return menuService.deleteMenu(id);
    }

    @GetMapping("/current")
    @Operation(summary = "Retrieve menu tree for the logged-in admin user")
    public ApiResponse<List<MenuResponse>> retrieveAuthenticatedUserMenus() {
        return menuService.retrieveAuthenticatedUserMenuTree();
    }
}
