package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.payload.response.LifeListCategoryResponse;
import space.nebula.nexus.payload.response.LifeListItemResponse;
import space.nebula.nexus.payload.response.LifeListPageResponse;
import space.nebula.nexus.service.ILifeListService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/life-list")
@RequiredArgsConstructor
@Tag(name = "Public Life List", description = "Public Before I Die list. Private and archived wishes are omitted.")
public class PublicLifeListController {

	private final ILifeListService lifeListService;

	@GetMapping("/categories")
	@Operation(summary = "List life-list categories")
	public ApiResponse<List<LifeListCategoryResponse>> getCategories() {
		return lifeListService.getPublicCategories();
	}

	@GetMapping
	@Operation(summary = "List public wishes", description = "Returns public, non-archived wishes. Summary counts use the same audience, not the current filter.")
	public ApiResponse<LifeListPageResponse> getItems(
			@Parameter(description = "Category slug") @RequestParam(required = false) String category,
			@Parameter(description = "Wish status") @RequestParam(required = false) LifeListStatus status,
			@PageableDefault(size = 100, sort = {"sortOrder",
					"number"}, direction = Sort.Direction.ASC) Pageable pageable) {
		return lifeListService.getPublicItems(category, status, pageable);
	}

	@GetMapping("/{number}")
	@Operation(summary = "Get a public wish by its stable number", description = "Private or missing numbers both return 404.")
	public ApiResponse<LifeListItemResponse> getItem(@PathVariable Integer number) {
		return lifeListService.getPublicItem(number);
	}
}
