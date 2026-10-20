package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.payload.request.LifeListCategoryRequest;
import space.nebula.nexus.payload.request.LifeListItemRequest;
import space.nebula.nexus.payload.request.LifeListMemoryRequest;
import space.nebula.nexus.payload.request.LifeListStatusRequest;
import space.nebula.nexus.payload.response.LifeListCategoryResponse;
import space.nebula.nexus.payload.response.LifeListItemResponse;
import space.nebula.nexus.payload.response.LifeListMemoryResponse;
import space.nebula.nexus.payload.response.LifeListPageResponse;
import space.nebula.nexus.service.ILifeListService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/life-list")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Life List", description = "Manage the Before I Die list")
public class AdminLifeListController {

	private final ILifeListService lifeListService;

	@GetMapping("/categories")
	public ApiResponse<List<LifeListCategoryResponse>> getCategories() {
		return lifeListService.getAdminCategories();
	}

	@PostMapping("/categories")
	@Operation(summary = "Create a category")
	public ApiResponse<LifeListCategoryResponse> createCategory(@Valid @RequestBody LifeListCategoryRequest request) {
		return lifeListService.createCategory(request);
	}

	@PutMapping("/categories/{id}")
	public ApiResponse<LifeListCategoryResponse> updateCategory(@PathVariable Long id,
			@Valid @RequestBody LifeListCategoryRequest request) {
		return lifeListService.updateCategory(id, request);
	}

	@DeleteMapping("/categories/{id}")
	public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
		return lifeListService.deleteCategory(id);
	}

	@GetMapping("/items")
	public ApiResponse<LifeListPageResponse> getItems(@RequestParam(required = false) String category,
			@RequestParam(required = false) LifeListStatus status, @RequestParam(required = false) Boolean archived,
			@PageableDefault(size = 100, sort = {"sortOrder",
					"number"}, direction = Sort.Direction.ASC) Pageable pageable) {
		return lifeListService.getAdminItems(category, status, archived, pageable);
	}

	@GetMapping("/items/{id}")
	public ApiResponse<LifeListItemResponse> getItem(@PathVariable Long id) {
		return lifeListService.getAdminItem(id);
	}

	@PostMapping("/items")
	public ApiResponse<LifeListItemResponse> createItem(@Valid @RequestBody LifeListItemRequest request) {
		return lifeListService.createItem(request);
	}

	@PutMapping("/items/{id}")
	public ApiResponse<LifeListItemResponse> updateItem(@PathVariable Long id,
			@Valid @RequestBody LifeListItemRequest request) {
		return lifeListService.updateItem(id, request);
	}

	@PatchMapping("/items/{id}/status")
	public ApiResponse<LifeListItemResponse> changeStatus(@PathVariable Long id,
			@Valid @RequestBody LifeListStatusRequest request) {
		return lifeListService.changeStatus(id, request);
	}

	@PostMapping("/items/{id}/archive")
	public ApiResponse<LifeListItemResponse> archiveItem(@PathVariable Long id) {
		return lifeListService.archiveItem(id);
	}

	@PostMapping("/items/{id}/restore")
	public ApiResponse<LifeListItemResponse> restoreItem(@PathVariable Long id) {
		return lifeListService.restoreItem(id);
	}

	@DeleteMapping("/items/{id}")
	public ApiResponse<Void> deleteItem(@PathVariable Long id) {
		return lifeListService.deleteItem(id);
	}

	@PostMapping("/items/{id}/memories")
	public ApiResponse<LifeListMemoryResponse> createMemory(@PathVariable Long id,
			@Valid @RequestBody LifeListMemoryRequest request) {
		return lifeListService.createMemory(id, request);
	}

	@PutMapping("/memories/{id}")
	public ApiResponse<LifeListMemoryResponse> updateMemory(@PathVariable Long id,
			@Valid @RequestBody LifeListMemoryRequest request) {
		return lifeListService.updateMemory(id, request);
	}

	@DeleteMapping("/memories/{id}")
	public ApiResponse<Void> deleteMemory(@PathVariable Long id) {
		return lifeListService.deleteMemory(id);
	}
}
