package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.RateLimit;
import space.nebula.nexus.entity.document.PostDocument;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.payload.response.QuickSearchResponse;
import space.nebula.nexus.payload.response.UnifiedSearchResponse;
import space.nebula.nexus.service.IPostSearchService;

/**
 * Controller for public full-text search operations. Leverages Elasticsearch to
 * provide high-performance search across blog content.
 */
@Tag(name = "Public Search", description = "Public full-text search endpoints for blog content")
@RestController
@RequestMapping("/api/v1/public/search")
@RequiredArgsConstructor
public class PublicSearchController
{

	private final IPostSearchService postSearchService;

	@GetMapping("/posts")
	@Operation(summary = "Search posts", description = "Perform a full-text search across published posts using Elasticsearch.")
	@RateLimit(count = 10, time = 1, message = "Search frequency too high. Please try again in a moment.")
	public ApiResponse<PageResult<PostDocument>> searchPosts(
			@Parameter(description = "Keywords to search for in title, summary, and content") @RequestParam(required = false) String keyword,

			@Parameter(description = "Pagination and sorting parameters") @PageableDefault(sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable)
	{
		return postSearchService.searchPosts(keyword, pageable);
	}

	@GetMapping("/quick")
	@Operation(summary = "Quick search (Command+K) - Legacy", description = "Lightweight unified search across posts, categories, and tags.")
	@RateLimit(count = 20, time = 1, message = "Quick search frequency too high.")
	public ApiResponse<QuickSearchResponse> quickSearch(
			@Parameter(description = "Search keyword") @RequestParam String keyword)
	{
		return postSearchService.quickSearch(keyword);
	}

	@GetMapping("/unified")
	@Operation(summary = "Unified search (Professional Cmd+K)", description = "Rich grouped search across all content with Next.js routing and metadata.")
	@RateLimit(count = 20, time = 1)
	public ApiResponse<UnifiedSearchResponse> unifiedSearch(
			@Parameter(description = "Search keyword") @RequestParam(required = false) String keyword)
	{
		return postSearchService.unifiedSearch(keyword);
	}

	@GetMapping("/suggestions")
	@Operation(summary = "Get search suggestions", description = "Provides autocomplete suggestions as the user types.")
	@RateLimit(count = 30, time = 1)
	public ApiResponse<java.util.List<String>> getSuggestions(
			@Parameter(description = "Search prefix") @RequestParam String keyword)
	{
		return postSearchService.getSearchSuggestions(keyword);
	}
}
