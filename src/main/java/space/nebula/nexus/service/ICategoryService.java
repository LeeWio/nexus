package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.CategoryRequest;
import space.nebula.nexus.payload.response.CategoryResponse;

import java.util.List;

public interface ICategoryService {

	/**
	 * Retrieves all available categories.
	 */
	ApiResponse<List<CategoryResponse>> retrieveAllCategories();

	ApiResponse<CategoryResponse> createCategory(CategoryRequest request);

	ApiResponse<CategoryResponse> updateCategory(Long id, CategoryRequest request);

	ApiResponse<Void> deleteCategory(Long id);
}
