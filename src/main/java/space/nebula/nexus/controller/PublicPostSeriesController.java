package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.SeriesResponse;
import space.nebula.nexus.service.IPostSeriesService;

import java.util.List;

/**
 * Controller for public access to blog series. Provides endpoints for browsing
 * post series and their ordered content.
 */
@Tag(name = "Public Post Series", description = "Public endpoints for exploring thematic blog series and columns")
@RestController
@RequestMapping("/api/v1/public/series")
@RequiredArgsConstructor
public class PublicPostSeriesController {

	private final IPostSeriesService seriesService;

	@GetMapping
	@Operation(summary = "Retrieve published series", description = "Fetch a list of all publicly available post series.")
	public ApiResponse<List<SeriesResponse>> retrievePublicSeries() {
		return seriesService.retrievePublicSeriesList();
	}

	@GetMapping("/{slug}")
	@Operation(summary = "Retrieve series details", description = "Fetch complete details for a specific series, including its ordered list of posts.")
	public ApiResponse<SeriesResponse> retrieveSeriesWithPosts(
			@Parameter(description = "The unique URL slug of the series") @PathVariable String slug) {
		return seriesService.retrieveSeriesWithPosts(slug);
	}
}
