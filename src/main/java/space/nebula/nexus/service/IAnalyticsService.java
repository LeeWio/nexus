package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.AnalyticsOverviewResponse;

public interface IAnalyticsService
{

	ApiResponse<AnalyticsOverviewResponse> retrieveOverviewStats();
}
