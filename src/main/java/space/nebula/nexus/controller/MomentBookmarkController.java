package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.MomentBookmarkImportRequest;
import space.nebula.nexus.payload.response.MomentResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IMomentService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/moments/saved")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Moment Bookmarks", description = "Private Moment bookmarks for the signed-in account")
public class MomentBookmarkController {

	private final IMomentService momentService;

	@GetMapping
	@Operation(summary = "List saved Moments", description = "Returns visible bookmarks for the current account, most recently saved first.")
	public ApiResponse<PageResult<MomentResponse>> getSavedMoments(@PageableDefault(size = 12) Pageable pageable) {
		return momentService.getSavedMoments(pageable);
	}

	@GetMapping("/ids")
	@Operation(summary = "Get saved Moment IDs", description = "Returns the visible bookmarked IDs for the current account.")
	public ApiResponse<List<Long>> getSavedMomentIds() {
		return momentService.getSavedMomentIds();
	}

	@PostMapping("/import")
	@Operation(summary = "Import legacy Moment bookmarks", description = "Adds up to 100 visible browser bookmarks the current account has never saved or removed. Unavailable Moments are skipped.")
	public ApiResponse<Void> importMomentBookmarks(@Valid @RequestBody MomentBookmarkImportRequest request) {
		return momentService.importMomentBookmarks(request.ids());
	}

	@PutMapping("/{id}")
	@Operation(summary = "Save a Moment", description = "Saves a visible Moment for the current account. Repeating the request preserves its position.")
	public ApiResponse<Void> saveMomentBookmark(@PathVariable Long id) {
		return momentService.saveMomentBookmark(id);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove a Moment bookmark", description = "Removes the current account's bookmark. Repeating the request is safe.")
	public ApiResponse<Void> removeMomentBookmark(@PathVariable Long id) {
		return momentService.removeMomentBookmark(id);
	}
}
