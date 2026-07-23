package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.MarketIndexResponse;

import java.util.List;

public interface IMarketDataService {
	/**
	 * Retrieves market indices for the requested period, using cached data when
	 * available.
	 *
	 * @param period
	 *            market data period
	 * @return API response containing the configured market indices
	 */
	ApiResponse<List<MarketIndexResponse>> getIndices(String period);

	/**
	 * Retrieves a single market index for the requested period.
	 *
	 * @param symbol
	 *            market index symbol
	 * @param period
	 *            market data period
	 * @return API response containing the matching market index
	 */
	ApiResponse<MarketIndexResponse> getIndex(String symbol, String period);

	/**
	 * Fetches fresh market indices from the upstream provider and replaces the
	 * cache.
	 *
	 * @param period
	 *            market data period
	 * @return number of market indices written to the cache
	 */
	int refreshIndices(String period);
}
