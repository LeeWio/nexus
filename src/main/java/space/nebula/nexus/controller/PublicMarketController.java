package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.MarketIndexResponse;
import space.nebula.nexus.service.IMarketDataService;

import java.util.List;

@Tag(name = "Public Market Data", description = "Public endpoints for market data and indices")
@RestController
@RequestMapping("/api/v1/public/market")
@RequiredArgsConstructor
public class PublicMarketController {

    private final IMarketDataService marketDataService;

    @Operation(summary = "Get market indices", description = "Fetches current data and sparkline for key market indices")
    @GetMapping("/indices")
    public ApiResponse<List<MarketIndexResponse>> getIndices(
            @RequestParam(defaultValue = "1D") String period
    ) {
        return marketDataService.getIndices(period);
    }

    @Operation(summary = "Get single market index", description = "Fetches current data and sparkline for a specific market index by symbol")
    @GetMapping("/indices/{symbol}")
    public ApiResponse<MarketIndexResponse> getIndex(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1D") String period
    ) {
        return marketDataService.getIndex(symbol, period);
    }
}
