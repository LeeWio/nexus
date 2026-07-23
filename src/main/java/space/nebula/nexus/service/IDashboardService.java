package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.DashboardStatsResponse;
import space.nebula.nexus.payload.response.PublicStatsResponse;

public interface IDashboardService {
	/**
	 * Get overall dashboard statistics for admins.
	 */
	ApiResponse<DashboardStatsResponse> getStatistics();

	/**
	 * Get public statistics for site visitors.
	 */
	ApiResponse<PublicStatsResponse> getPublicStatistics();
}
