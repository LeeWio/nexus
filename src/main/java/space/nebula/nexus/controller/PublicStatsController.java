package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.PublicStatsResponse;
import space.nebula.nexus.service.IDashboardService;

@Tag(name = "Public Statistics", description = "Public endpoints for site-wide statistics")
@RestController
@RequestMapping("/api/v1/public/stats")
@RequiredArgsConstructor
public class PublicStatsController {

	private final IDashboardService dashboardService;
	private final space.nebula.nexus.service.IAnalyticsService analyticsService;

	@Operation(summary = "Get site statistics", description = "Returns non-sensitive site metrics for the front-end dashboard")
	@GetMapping
	public ApiResponse<PublicStatsResponse> getPublicStatistics() {
		return dashboardService.getPublicStatistics();
	}

	@Operation(summary = "Get trending posts", description = "Returns a list of popular blog posts based on recent traffic.")
	@GetMapping("/trending")
	public ApiResponse<java.util.List<space.nebula.nexus.payload.response.PostResponse>> getTrending(
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "5") int limit) {
		return analyticsService.getTrendingPosts(limit);
	}
}
