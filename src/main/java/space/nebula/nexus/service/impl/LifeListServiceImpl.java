package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.LifeListCategory;
import space.nebula.nexus.entity.LifeListItem;
import space.nebula.nexus.entity.LifeListMemory;
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.enums.LifeListVisibility;
import space.nebula.nexus.payload.request.LifeListCategoryRequest;
import space.nebula.nexus.payload.request.LifeListItemRequest;
import space.nebula.nexus.payload.request.LifeListMemoryRequest;
import space.nebula.nexus.payload.request.LifeListStatusRequest;
import space.nebula.nexus.payload.response.LifeListCategoryResponse;
import space.nebula.nexus.payload.response.LifeListItemResponse;
import space.nebula.nexus.payload.response.LifeListMemoryResponse;
import space.nebula.nexus.payload.response.LifeListPageResponse;
import space.nebula.nexus.payload.response.LifeListSummaryResponse;
import space.nebula.nexus.repository.LifeListCategoryRepository;
import space.nebula.nexus.repository.LifeListItemRepository;
import space.nebula.nexus.repository.LifeListMemoryRepository;
import space.nebula.nexus.service.ILifeListService;
import space.nebula.nexus.utils.LifeListStatusPolicy;
import space.nebula.nexus.utils.SlugUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class LifeListServiceImpl implements ILifeListService {

	private final LifeListCategoryRepository categoryRepository;
	private final LifeListItemRepository itemRepository;
	private final LifeListMemoryRepository memoryRepository;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<LifeListCategoryResponse>> getPublicCategories() {
		return ApiResponse.success(categories());
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<LifeListPageResponse> getPublicItems(String categorySlug, LifeListStatus status,
			Pageable pageable) {
		return ApiResponse.success(toPage(findItems(publicSpec(categorySlug, status), pageable), false, false));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<LifeListItemResponse> getPublicItem(Integer number) {
		LifeListItem item = itemRepository.findByNumber(number)
				.filter(found -> found.getVisibility() == LifeListVisibility.PUBLIC && found.getArchivedAt() == null)
				.orElseThrow(() -> new ResourceNotFoundException("Life list item", "number", number));
		return ApiResponse.success(toItem(item, false));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<LifeListCategoryResponse>> getAdminCategories() {
		return ApiResponse.success(categories());
	}

	@Override
	@Transactional
	@LogOperation("Create Life List Category")
	public ApiResponse<LifeListCategoryResponse> createCategory(LifeListCategoryRequest request) {
		LifeListCategory category = new LifeListCategory();
		applyCategory(category, request, true);
		categoryRepository.save(category);
		return ApiResponse.success("Category created", toCategory(category));
	}

	@Override
	@Transactional
	@LogOperation("Update Life List Category")
	public ApiResponse<LifeListCategoryResponse> updateCategory(Long id, LifeListCategoryRequest request) {
		LifeListCategory category = category(id);
		applyCategory(category, request, false);
		return ApiResponse.success("Category updated", toCategory(categoryRepository.save(category)));
	}

	@Override
	@Transactional
	@LogOperation("Delete Life List Category")
	public ApiResponse<Void> deleteCategory(Long id) {
		LifeListCategory category = category(id);
		if ("uncategorized".equals(category.getSlug())) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "The uncategorized category cannot be deleted");
		}
		if (itemRepository.countByCategoryId(id) > 0) {
			throw new BusinessException(BusinessCode.BAD_REQUEST,
					"Move wishes out of this category before deleting it");
		}
		categoryRepository.delete(category);
		return ApiResponse.success("Category deleted", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<LifeListPageResponse> getAdminItems(String categorySlug, LifeListStatus status, Boolean archived,
			Pageable pageable) {
		boolean includeArchived = Boolean.TRUE.equals(archived);
		Specification<LifeListItem> spec = andAll(categorySpec(categorySlug), statusSpec(status));
		if (!includeArchived) {
			spec = spec.and((root, query, builder) -> builder.isNull(root.get("archivedAt")));
		}
		return ApiResponse.success(toPage(findItems(spec, pageable), true, includeArchived));
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<LifeListItemResponse> getAdminItem(Long id) {
		return ApiResponse.success(toItem(item(id), true));
	}

	@Override
	@Transactional
	@LogOperation("Create Life List Item")
	public ApiResponse<LifeListItemResponse> createItem(LifeListItemRequest request) {
		LifeListItem item = new LifeListItem();
		item.setNumber(itemRepository.findMaxNumber() + 1);
		applyItem(item, request);
		if (item.getSortOrder() == null || item.getSortOrder() == 0) {
			item.setSortOrder(itemRepository.findMaxSortOrder() + 1);
		}
		itemRepository.save(item);
		log.info("Life list item created number={}", item.getNumber());
		return ApiResponse.success("Wish created", toItem(item, true));
	}

	@Override
	@Transactional
	@LogOperation("Update Life List Item")
	public ApiResponse<LifeListItemResponse> updateItem(Long id, LifeListItemRequest request) {
		LifeListItem item = item(id);
		applyItem(item, request);
		return ApiResponse.success("Wish updated", toItem(itemRepository.save(item), true));
	}

	@Override
	@Transactional
	@LogOperation("Change Life List Status")
	public ApiResponse<LifeListItemResponse> changeStatus(Long id, LifeListStatusRequest request) {
		LifeListItem item = item(id);
		if (!LifeListStatusPolicy.canTransition(item.getStatus(), request.status())) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "That status change is not allowed");
		}
		applyStatus(item, request.status(), request.completedAt());
		return ApiResponse.success("Status updated", toItem(itemRepository.save(item), true));
	}

	@Override
	@Transactional
	@LogOperation("Archive Life List Item")
	public ApiResponse<LifeListItemResponse> archiveItem(Long id) {
		LifeListItem item = item(id);
		if (item.getArchivedAt() == null) {
			item.setArchivedAt(LocalDateTime.now());
		}
		return ApiResponse.success("Wish archived", toItem(itemRepository.save(item), true));
	}

	@Override
	@Transactional
	@LogOperation("Restore Life List Item")
	public ApiResponse<LifeListItemResponse> restoreItem(Long id) {
		LifeListItem item = item(id);
		item.setArchivedAt(null);
		return ApiResponse.success("Wish restored", toItem(itemRepository.save(item), true));
	}

	@Override
	@Transactional
	@LogOperation("Delete Life List Item")
	public ApiResponse<Void> deleteItem(Long id) {
		LifeListItem item = item(id);
		memoryRepository.findByItemIdOrderByOccurredAtDescCreatedAtDesc(item.getId()).forEach(memoryRepository::delete);
		itemRepository.delete(item);
		return ApiResponse.success("Wish deleted", null);
	}

	@Override
	@Transactional
	@LogOperation("Create Life List Memory")
	public ApiResponse<LifeListMemoryResponse> createMemory(Long itemId, LifeListMemoryRequest request) {
		LifeListMemory memory = new LifeListMemory();
		memory.setItem(item(itemId));
		applyMemory(memory, request);
		memoryRepository.save(memory);
		return ApiResponse.success("Memory saved", toMemory(memory));
	}

	@Override
	@Transactional
	@LogOperation("Update Life List Memory")
	public ApiResponse<LifeListMemoryResponse> updateMemory(Long memoryId, LifeListMemoryRequest request) {
		LifeListMemory memory = memory(memoryId);
		applyMemory(memory, request);
		return ApiResponse.success("Memory updated", toMemory(memoryRepository.save(memory)));
	}

	@Override
	@Transactional
	@LogOperation("Delete Life List Memory")
	public ApiResponse<Void> deleteMemory(Long memoryId) {
		memoryRepository.delete(memory(memoryId));
		return ApiResponse.success("Memory deleted", null);
	}

	private Page<LifeListItem> findItems(Specification<LifeListItem> spec, Pageable pageable) {
		if (spec == null) {
			return itemRepository.findAll(pageable);
		}
		return itemRepository.findAll(spec, pageable);
	}

	private Specification<LifeListItem> publicSpec(String categorySlug, LifeListStatus status) {
		return andAll(categorySpec(categorySlug), statusSpec(status),
				(root, query, builder) -> builder.equal(root.get("visibility"), LifeListVisibility.PUBLIC),
				(root, query, builder) -> builder.isNull(root.get("archivedAt")));
	}

	private Specification<LifeListItem> andAll(Specification<LifeListItem>... parts) {
		Specification<LifeListItem> combined = null;
		for (Specification<LifeListItem> part : parts) {
			if (part == null) {
				continue;
			}
			combined = combined == null ? part : combined.and(part);
		}
		return combined;
	}

	private Specification<LifeListItem> categorySpec(String categorySlug) {
		if (categorySlug == null || categorySlug.isBlank()) {
			return null;
		}
		return (root, query, builder) -> builder.equal(root.get("category").get("slug"), categorySlug);
	}

	private Specification<LifeListItem> statusSpec(LifeListStatus status) {
		if (status == null) {
			return null;
		}
		return (root, query, builder) -> builder.equal(root.get("status"), status);
	}

	private void applyCategory(LifeListCategory category, LifeListCategoryRequest request, boolean creating) {
		String name = request.name().trim();
		if (categoryRepository.existsByName(name) && (creating || !name.equals(category.getName()))) {
			throw new BusinessException(BusinessCode.DUPLICATE_KEY, "A category with that name already exists");
		}
		category.setName(name);
		String slug = resolveSlug(request.slug(), name, category.getId());
		category.setSlug(slug);
		if (request.sortOrder() != null) {
			category.setSortOrder(request.sortOrder());
		} else if (creating) {
			category.setSortOrder(categoryRepository.findMaxSortOrder() + 10);
		}
	}

	private String resolveSlug(String requested, String name, Long currentId) {
		String base = requested == null || requested.isBlank() ? SlugUtil.toSlug(name) : SlugUtil.toSlug(requested);
		if (base.isBlank()) {
			base = "category";
		}
		String candidate = base;
		int suffix = 2;
		while (slugTaken(candidate, currentId)) {
			candidate = base + "-" + suffix++;
		}
		return candidate.toLowerCase(Locale.ROOT);
	}

	private boolean slugTaken(String slug, Long currentId) {
		return categoryRepository.findBySlug(slug).filter(found -> !found.getId().equals(currentId)).isPresent();
	}

	private void applyItem(LifeListItem item, LifeListItemRequest request) {
		item.setTitle(request.title().trim());
		item.setDescription(blankToNull(request.description()));
		item.setMotivation(blankToNull(request.motivation()));
		item.setVisibility(request.visibility());
		if (request.sortOrder() != null) {
			item.setSortOrder(request.sortOrder());
		}
		if (request.categoryId() == null) {
			item.setCategory(null);
		} else {
			item.setCategory(category(request.categoryId()));
		}
		if (!LifeListStatusPolicy.canTransition(item.getStatus() == null ? LifeListStatus.PLANNED : item.getStatus(),
				request.status())) {
			throw new BusinessException(BusinessCode.BAD_REQUEST, "That status change is not allowed");
		}
		applyStatus(item, request.status(), request.completedAt());
	}

	private void applyStatus(LifeListItem item, LifeListStatus status, LocalDate completedAt) {
		item.setStatus(status);
		if (status == LifeListStatus.COMPLETED) {
			item.setCompletedAt(LifeListStatusPolicy.nextCompletedAt(status, completedAt, LocalDate.now()));
			return;
		}
		if (LifeListStatusPolicy.clearsCompletedAt(status)) {
			item.setCompletedAt(null);
		}
	}

	private void applyMemory(LifeListMemory memory, LifeListMemoryRequest request) {
		memory.setContent(request.content().trim());
		memory.setLocation(blankToNull(request.location()));
		memory.setOccurredAt(request.occurredAt());
		memory.setVisibility(request.visibility());
	}

	private LifeListPageResponse toPage(Page<LifeListItem> page, boolean includePrivate, boolean includeArchived) {
		List<LifeListItemResponse> list = page.getContent().stream().map(item -> toItem(item, includePrivate)).toList();
		LifeListSummaryResponse summary = includePrivate
				? new LifeListSummaryResponse(itemRepository.countByStatusAndArchivedAtIsNull(LifeListStatus.COMPLETED),
						itemRepository.countByArchivedAtIsNull(), true)
				: new LifeListSummaryResponse(
						itemRepository.countByVisibilityAndStatusAndArchivedAtIsNull(LifeListVisibility.PUBLIC,
								LifeListStatus.COMPLETED),
						itemRepository.countByVisibilityAndArchivedAtIsNull(LifeListVisibility.PUBLIC), false);
		return new LifeListPageResponse(list, page.getTotalElements(), page.getNumber() + 1, page.getSize(),
				page.getTotalPages(), summary);
	}

	private LifeListItemResponse toItem(LifeListItem item, boolean includePrivate) {
		List<LifeListMemory> memories = includePrivate
				? memoryRepository.findByItemIdOrderByOccurredAtDescCreatedAtDesc(item.getId())
				: memoryRepository.findByItemIdAndVisibilityOrderByOccurredAtDescCreatedAtDesc(item.getId(),
						LifeListVisibility.PUBLIC);
		return new LifeListItemResponse(item.getId(), item.getNumber(), item.getTitle(), item.getDescription(),
				item.getMotivation(), item.getCategory() == null ? null : toCategory(item.getCategory()),
				item.getStatus(), item.getVisibility(), item.getSortOrder() == null ? 0 : item.getSortOrder(),
				item.getCompletedAt(), item.getArchivedAt(), memories.size(),
				memories.stream().map(this::toMemory).toList(), item.getCreatedAt(), item.getUpdatedAt());
	}

	private LifeListMemoryResponse toMemory(LifeListMemory memory) {
		return new LifeListMemoryResponse(memory.getId(), memory.getItem().getId(), memory.getOccurredAt(),
				memory.getContent(), memory.getLocation(), memory.getVisibility(), memory.getCreatedAt(),
				memory.getUpdatedAt());
	}

	private LifeListCategoryResponse toCategory(LifeListCategory category) {
		return new LifeListCategoryResponse(category.getId(), category.getName(), category.getSlug(),
				category.getSortOrder() == null ? 0 : category.getSortOrder());
	}

	private List<LifeListCategoryResponse> categories() {
		return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream().map(this::toCategory).toList();
	}

	private LifeListCategory category(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Life list category", "id", id));
	}

	private LifeListItem item(Long id) {
		return itemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Life list item", "id", id));
	}

	private LifeListMemory memory(Long id) {
		return memoryRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Life list memory", "id", id));
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
