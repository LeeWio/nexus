package space.nebula.nexus.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.payload.response.MarketIndexResponse;
import space.nebula.nexus.service.IMarketDataService;
import space.nebula.nexus.service.impl.MarketDataServiceImpl;
import space.nebula.nexus.utils.RedisUtil;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class MarketDataScheduledTask {

    private final IMarketDataService marketDataService;
    private final RedisUtil redisUtil;

    /**
     * Pre-warm cache for 1D period every 1 minute.
     * This avoids cold starts when users first load the dashboard.
     */
    @Scheduled(cron = "0 * * * * *")
    public void preWarmMarketData1D() {
        log.info("Starting scheduled pre-warming of 1D market data");
        try {
            if (marketDataService instanceof MarketDataServiceImpl impl) {
                List<MarketIndexResponse> responses = impl.fetchIndicesFromApi(CacheConstants.MARKET_1D);
                if (!responses.isEmpty()) {
                    String cacheKey = CacheConstants.buildFullKey(CacheConstants.MARKET_INDICES, CacheConstants.MARKET_1D);
                    redisUtil.set(cacheKey, responses, 1, TimeUnit.MINUTES);
                    log.info("Successfully pre-warmed 1D market data for {} indices", responses.size());
                }
            }
        } catch (Exception e) {
            log.error("Failed to pre-warm 1D market data", e);
        }
    }
}
