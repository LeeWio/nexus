package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.AnalyticsOverviewResponse;

public interface IAnalyticsService
{

	ApiResponse<AnalyticsOverviewResponse> retrieveOverviewStats();

	/**
	 * Aggregates logs for a specific date and saves to DailyAnalytics.
	 */
	void aggregateDailyData(java.time.LocalDate date);

	/**
	 * Retrieves top trending posts based on recent views.
	 */
	ApiResponse<java.util.List<space.nebula.nexus.payload.response.PostResponse>> getTrendingPosts(int limit);
}
