package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.TagRequest;
import space.nebula.nexus.payload.response.TagResponse;
import space.nebula.nexus.service.ITagService;

import java.util.List;

/**
 * Controller for administrative blog tag management. Provides endpoints for
 * managing keywords and labels used to categorize content.
 */
@Tag(name = "Admin Tag Management", description = "Endpoints for managing blog tags and keywords")
@RestController
@RequestMapping("/api/v1/admin/tags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class TagController
{

	private final ITagService tagService;

	@GetMapping
	@Operation(summary = "Get all tags", description = "Retrieve a comprehensive list of all blog tags.")
	public ApiResponse<List<TagResponse>> getAllTags()
	{
		return tagService.getAllTags();
	}

	@PostMapping
	@Operation(summary = "Create tag", description = "Add a new tag keyword with a unique slug.")
	public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagRequest request)
	{
		return tagService.createTag(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update tag", description = "Modify an existing tag's name or slug.")
	public ApiResponse<TagResponse> updateTag(@Parameter(description = "Tag ID") @PathVariable Long id,
			@Valid @RequestBody TagRequest request)
	{
		return tagService.updateTag(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete tag", description = "Permanently remove a tag from the system.")
	public ApiResponse<Void> deleteTag(@Parameter(description = "Tag ID") @PathVariable Long id)
	{
		return tagService.deleteTag(id);
	}
}
