package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.service.IMarketDataService;

@Component
@Slf4j
@RequiredArgsConstructor
public class MarketDataScheduledTask
{

	private final IMarketDataService marketDataService;

	/**
	 * Pre-warm cache for 1D period every 1 minute. This avoids cold starts when
	 * users first load the dashboard.
	 */
	@Scheduled(cron = "0 * * * * *")
	@SchedulerLock(name = "marketDataPreWarm", lockAtMostFor = "PT50S")
	public void preWarmMarketData1D()
	{
		log.info("Starting scheduled pre-warming of 1D market data");
		try
		{
			int refreshedCount = marketDataService.refreshIndices(CacheConstants.MARKET_1D);
			if (refreshedCount > 0)
			{
				log.info("Successfully pre-warmed 1D market data for {} indices", refreshedCount);
			}
		}
		catch (Exception e)
		{
			log.error("Failed to pre-warm 1D market data", e);
		}
	}
}
