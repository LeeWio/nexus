package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.MarketIndexResponse;
import space.nebula.nexus.payload.response.StockSearchResponse;
import space.nebula.nexus.payload.response.StockTrendResponse;

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

	/**
	 * Searches stocks matching the given keyword using Sina Suggest API.
	 *
	 * @param keyword
	 *            stock code, name or pinyin
	 * @return API response containing the list of matching stocks
	 */
	ApiResponse<List<StockSearchResponse>> searchStocks(String keyword);

	/**
	 * Retrieves a stock's historical trend and real-time details.
	 *
	 * @param symbol
	 *            stock symbol (e.g., sh600519, AAPL)
	 * @param period
	 *            requested period (1W, 1M, 1Y)
	 * @return API response containing the stock trend and real-time data
	 */
	ApiResponse<StockTrendResponse> getStockTrend(String symbol, String period);
}
