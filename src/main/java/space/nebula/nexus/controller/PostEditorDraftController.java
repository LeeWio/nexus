package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.service.PostEditorDraftService;
import space.nebula.nexus.service.PostEditorDraftService.SaveDraft;
import space.nebula.nexus.service.PostEditorDraftService.Snapshot;

@RestController
@RequestMapping("/api/v1/admin/posts/editor-drafts")
@PreAuthorize("isAuthenticated() and (#identifier == 'new' or hasPermission(#identifier, 'Post', 'EDIT'))")
@RequiredArgsConstructor
public class PostEditorDraftController {
	private final PostEditorDraftService drafts;

	@GetMapping("/{identifier}")
	@Operation(summary = "Retrieve the current user's article editor draft")
	public ApiResponse<Snapshot> retrieve(
			@PathVariable @Pattern(regexp = PostEditorDraftService.IDENTIFIER) String identifier) {
		return ApiResponse.success(drafts.retrieve(identifier));
	}

	@PutMapping("/{identifier}")
	@Operation(summary = "Save a private article editor draft")
	public ApiResponse<Snapshot> save(
			@PathVariable @Pattern(regexp = PostEditorDraftService.IDENTIFIER) String identifier,
			@Valid @RequestBody SaveDraft request) {
		return ApiResponse.success(drafts.save(identifier, request));
	}

	@DeleteMapping("/{identifier}")
	@Operation(summary = "Discard a private article editor draft")
	public ApiResponse<Void> discard(
			@PathVariable @Pattern(regexp = PostEditorDraftService.IDENTIFIER) String identifier,
			@RequestParam(required = false) @Size(max = 36) String revision) {
		drafts.discard(identifier, revision);
		return ApiResponse.success();
	}
}
