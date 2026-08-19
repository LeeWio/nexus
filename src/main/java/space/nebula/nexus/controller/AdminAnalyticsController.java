package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.AnalyticsOverviewResponse;
import space.nebula.nexus.payload.response.ContentFunnelResponse;
import space.nebula.nexus.service.IAnalyticsService;

@Tag(name = "Admin Analytics", description = "Endpoints for website traffic analysis and insights")
@RestController
@RequestMapping(space.nebula.nexus.common.constant.ApiConstants.ADMIN + "/analytics")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAnalyticsController {

	private final IAnalyticsService analyticsService;

	@Operation(summary = "Retrieve today's traffic overview", description = "Return headline traffic counters and comparison values used by the administrative analytics landing view.")
	@GetMapping("/overview")
	public ApiResponse<AnalyticsOverviewResponse> retrieveOverview() {
		return analyticsService.retrieveOverviewStats();
	}

	@Operation(summary = "Retrieve top pages statistics", description = "Return the most visited page routes with views, average time, bounce rate, and trend values for analytics tables.")
	@GetMapping("/top-pages")
	public ApiResponse<java.util.List<space.nebula.nexus.payload.response.TopPageResponse>> getTopPages() {
		return analyticsService.getTopPages();
	}

	@Operation(summary = "Retrieve traffic statistics", description = "Return traffic time series and breakdowns by device and referrer source for the requested rolling number of days.")
	@GetMapping("/traffic")
	public ApiResponse<space.nebula.nexus.payload.response.TrafficStatsResponse> getTrafficStats(
			@Parameter(description = "Rolling lookback window in days", example = "30") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "30") int days) {
		return analyticsService.getTrafficStats(days);
	}

	@Operation(summary = "Retrieve trending posts", description = "Return the currently popular posts ordered by recent traffic, intended for administrative dashboards rather than the public discovery feed.")
	@GetMapping("/trending")
	public ApiResponse<java.util.List<space.nebula.nexus.payload.response.PostResponse>> getTrending(
			@Parameter(description = "Maximum number of trending posts to return", example = "10") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit) {
		return analyticsService.getTrendingPosts(limit);
	}

	@Operation(summary = "Retrieve the content conversion funnel", description = "Returns anonymous post impressions, clicks, reading milestones, durable interactions, verified subscriptions, and returning visitors.")
	@GetMapping("/content-funnel")
	public ApiResponse<ContentFunnelResponse> getContentFunnel(
			@Parameter(description = "Rolling lookback window in days", example = "30") @org.springframework.web.bind.annotation.RequestParam(defaultValue = "30") int days,
			@Parameter(description = "Optional post identifier") @org.springframework.web.bind.annotation.RequestParam(required = false) Long postId) {
		return analyticsService.getContentFunnel(days, postId);
	}
}
