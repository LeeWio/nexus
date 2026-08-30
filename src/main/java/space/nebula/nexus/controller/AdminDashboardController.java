package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.DashboardStatsResponse;
import space.nebula.nexus.payload.response.ContentOperationsOverviewResponse;
import space.nebula.nexus.service.IDashboardService;
import space.nebula.nexus.service.IContentOperationsService;

/**
 * Controller for administrative dashboard data. Provides high-level statistics
 * and aggregated information for the management interface.
 */
@Tag(name = "Admin Dashboard", description = "Endpoints for administrative dashboard metrics and overview")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

	private final IDashboardService dashboardService;
	private final IContentOperationsService contentOperationsService;

	@GetMapping("/stats")
	@Operation(summary = "Get Dashboard Statistics", description = "Retrieve overall statistics including post counts, user activity, and system status.")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<DashboardStatsResponse> getStatistics() {
		return dashboardService.getStatistics();
	}

	@GetMapping("/content-overview")
	@Operation(summary = "Get content operations overview", description = "Retrieve a content-first operational snapshot for the administrative workspace.")
	@PreAuthorize("hasRole('ADMIN')")
	public ApiResponse<ContentOperationsOverviewResponse> getContentOverview() {
		return contentOperationsService.getOverview();
	}
}
