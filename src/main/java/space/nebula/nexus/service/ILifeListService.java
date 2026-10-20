package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
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

import java.util.List;

public interface ILifeListService {

	ApiResponse<List<LifeListCategoryResponse>> getPublicCategories();

	ApiResponse<LifeListPageResponse> getPublicItems(String categorySlug, LifeListStatus status, Pageable pageable);

	ApiResponse<LifeListItemResponse> getPublicItem(Integer number);

	ApiResponse<List<LifeListCategoryResponse>> getAdminCategories();

	ApiResponse<LifeListCategoryResponse> createCategory(LifeListCategoryRequest request);

	ApiResponse<LifeListCategoryResponse> updateCategory(Long id, LifeListCategoryRequest request);

	ApiResponse<Void> deleteCategory(Long id);

	ApiResponse<LifeListPageResponse> getAdminItems(String categorySlug, LifeListStatus status, Boolean archived,
			Pageable pageable);

	ApiResponse<LifeListItemResponse> getAdminItem(Long id);

	ApiResponse<LifeListItemResponse> createItem(LifeListItemRequest request);

	ApiResponse<LifeListItemResponse> updateItem(Long id, LifeListItemRequest request);

	ApiResponse<LifeListItemResponse> changeStatus(Long id, LifeListStatusRequest request);

	ApiResponse<LifeListItemResponse> archiveItem(Long id);

	ApiResponse<LifeListItemResponse> restoreItem(Long id);

	ApiResponse<Void> deleteItem(Long id);

	ApiResponse<LifeListMemoryResponse> createMemory(Long itemId, LifeListMemoryRequest request);

	ApiResponse<LifeListMemoryResponse> updateMemory(Long memoryId, LifeListMemoryRequest request);

	ApiResponse<Void> deleteMemory(Long memoryId);
}
