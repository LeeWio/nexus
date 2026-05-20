package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.CategoryRequest;
import space.nebula.nexus.payload.response.CategoryResponse;
import space.nebula.nexus.service.ICategoryService;

import java.util.List;

/**
 * Controller for administrative blog category management.
 * Provides endpoints for organizing blog posts into hierarchical or flat categories.
 */
@Tag(name = "Admin Category Management", description = "Endpoints for managing blog categories and classification")
@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {

    private final ICategoryService categoryService;

    @GetMapping
    @Operation(summary = "Retrieve all categories", description = "Fetch a complete list of all blog categories.")
    public ApiResponse<List<CategoryResponse>> retrieveCategories() {
        return categoryService.retrieveAllCategories();
    }

    @PostMapping
    @Operation(summary = "Create category", description = "Add a new category with a unique name and slug.")
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category", description = "Modify an existing category's metadata, name, or slug.")
    public ApiResponse<CategoryResponse> updateCategory(
            @Parameter(description = "Category ID") @PathVariable Long id, 
            @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete category", description = "Permanently remove a category from the system.")
    public ApiResponse<Void> deleteCategory(@Parameter(description = "Category ID") @PathVariable Long id) {
        return categoryService.deleteCategory(id);
    }
}
