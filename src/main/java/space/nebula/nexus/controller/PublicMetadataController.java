package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.CategoryResponse;
import space.nebula.nexus.payload.response.TagResponse;
import space.nebula.nexus.service.ICategoryService;
import space.nebula.nexus.service.ITagService;

import java.util.List;

/**
 * Publicly accessible endpoints for categories and tags.
 */
@Tag(name = "Public Metadata API", description = "Public endpoints for categories and tags")
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicMetadataController
{

	private final ICategoryService categoryService;
	private final ITagService tagService;

	@GetMapping("/categories")
	@Operation(summary = "Retrieve all categories", description = "Fetch a complete list of all blog categories.")
	public ApiResponse<List<CategoryResponse>> retrieveCategories()
	{
		return categoryService.retrieveAllCategories();
	}

	@GetMapping("/tags")
	@Operation(summary = "Get all tags", description = "Retrieve a comprehensive list of all blog tags.")
	public ApiResponse<List<TagResponse>> getAllTags()
	{
		return tagService.getAllTags();
	}
}
