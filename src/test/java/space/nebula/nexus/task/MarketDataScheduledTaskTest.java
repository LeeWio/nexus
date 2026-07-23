package space.nebula.nexus.task;

import org.junit.jupiter.api.Test;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.service.IMarketDataService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MarketDataScheduledTaskTest {
	@Test
	void preWarmMarketData1DDelegatesRefreshToService() {
		IMarketDataService marketDataService = mock(IMarketDataService.class);
		MarketDataScheduledTask task = new MarketDataScheduledTask(marketDataService);

		task.preWarmMarketData1D();

		verify(marketDataService).refreshIndices(CacheConstants.MARKET_1D);
	}
}
