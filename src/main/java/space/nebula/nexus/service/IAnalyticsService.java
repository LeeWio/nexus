package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.AnalyticsOverviewResponse;
import space.nebula.nexus.payload.request.ContentAnalyticsEventRequest;
import space.nebula.nexus.payload.response.ContentFunnelResponse;

public interface IAnalyticsService {

	ApiResponse<AnalyticsOverviewResponse> retrieveOverviewStats();

	/**
	 * Retrieves top pages statistics with measured session duration and bounce
	 * rate.
	 */
	ApiResponse<java.util.List<space.nebula.nexus.payload.response.TopPageResponse>> getTopPages();

	/**
	 * Retrieves traffic statistics broken down by device and source.
	 */
	ApiResponse<space.nebula.nexus.payload.response.TrafficStatsResponse> getTrafficStats(int days);

	/**
	 * Aggregates logs for a specific date and saves to DailyAnalytics.
	 */
	void aggregateDailyData(java.time.LocalDate date);

	/**
	 * Retrieves top trending posts based on recent views.
	 */
	ApiResponse<java.util.List<space.nebula.nexus.payload.response.PostResponse>> getTrendingPosts(int limit);

	/** Records a privacy-preserving, de-duplicated post engagement milestone. */
	ApiResponse<Void> recordContentEvent(ContentAnalyticsEventRequest request, String sessionId, String visitorHash);

	/** Retrieves the discovery-to-conversion funnel for all content or one post. */
	ApiResponse<ContentFunnelResponse> getContentFunnel(int days, Long postId);
	/**
	 * Purges old visit logs that have already been aggregated. Typically keeps logs
	 * for the last 90 days.
	 */
	void purgeOldLogs(int daysToKeep);
}
