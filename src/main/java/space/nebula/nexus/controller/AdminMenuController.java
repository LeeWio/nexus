package space.nebula.nexus.controller;
import lombok.RequiredArgsConstructor;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MenuRequest;
import space.nebula.nexus.payload.response.MenuResponse;
import space.nebula.nexus.service.IMenuService;

import java.util.List;

@RequiredArgsConstructor

@RestController

@RequestMapping("/api/v1/admin/menus")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Menu Management", description = "Endpoints for managing system menus and permissions")
public class AdminMenuController {

    
    private final IMenuService menuService;

    @GetMapping("/tree")
    @Operation(summary = "Get full menu tree")
    public ApiResponse<List<MenuResponse>> getMenuTree() {
        return menuService.getMenuTree();
    }

    @PostMapping
    @Operation(summary = "Create a new menu")
    public ApiResponse<MenuResponse> createMenu(@Valid @RequestBody MenuRequest request) {
        return menuService.createMenu(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing menu")
    public ApiResponse<MenuResponse> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuRequest request) {
        return menuService.updateMenu(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a menu")
    public ApiResponse<Void> deleteMenu(@PathVariable Long id) {
        return menuService.deleteMenu(id);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current user's menu tree")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<MenuResponse>> getCurrentUserMenuTree() {
        return menuService.getCurrentUserMenuTree();
    }
}
