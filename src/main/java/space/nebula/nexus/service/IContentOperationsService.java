package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.ContentOperationsOverviewResponse;

public interface IContentOperationsService {
	ApiResponse<ContentOperationsOverviewResponse> getOverview();
}
