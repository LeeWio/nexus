package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.AnalyticsOverviewResponse;
import space.nebula.nexus.service.IAnalyticsService;

@Tag(name = "Admin Analytics", description = "Endpoints for website traffic analysis and insights")
@RestController
@RequestMapping("/api/v1/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAnalyticsController
{

	private final IAnalyticsService analyticsService;

	@Operation(summary = "Retrieve today's traffic overview")
	@GetMapping("/overview")
	public ApiResponse<AnalyticsOverviewResponse> retrieveOverview()
	{
		return analyticsService.retrieveOverviewStats();
	}

	@Operation(summary = "Retrieve trending posts", description = "Get a list of currently popular posts based on recent traffic.")
	@GetMapping("/trending")
	public ApiResponse<java.util.List<space.nebula.nexus.payload.response.PostResponse>> getTrending(
			@org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit)
	{
		return analyticsService.getTrendingPosts(limit);
	}
}
