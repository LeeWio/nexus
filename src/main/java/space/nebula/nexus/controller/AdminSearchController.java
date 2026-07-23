package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.service.IPostSearchService;

/**
 * Controller for administrative search index management.
 */
@Tag(name = "Admin Search Management", description = "Endpoints for managing search indices and synchronization")
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSearchController {

	private final IPostSearchService postSearchService;

	@PostMapping("/rebuild")
	@Operation(summary = "Rebuild search index", description = "Trigger a background task to clear and rebuild the entire search index from the database.")
	public ApiResponse<Void> rebuildIndex() {
		postSearchService.rebuildIndex();
		return ApiResponse.success("Search index rebuild task started in the background", null);
	}
}
