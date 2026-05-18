package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.MarketIndexResponse;

import java.util.List;

public interface IMarketDataService {
    ApiResponse<List<MarketIndexResponse>> getIndices();
}
