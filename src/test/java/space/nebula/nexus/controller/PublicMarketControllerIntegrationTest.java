package space.nebula.nexus.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.payload.response.StockSearchResponse;
import space.nebula.nexus.payload.response.StockTrendResponse;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.IMarketDataService;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PublicMarketControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private IMarketDataService marketDataService;

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Test
	void searchStocks_ReturnsList() throws Exception {
		List<StockSearchResponse> mockList = List.of(
				StockSearchResponse.builder().name("贵州茅台").code("600519").symbol("sh600519").market("CN").build(),
				StockSearchResponse.builder().name("Apple").code("AAPL").symbol("gb_aapl").market("US").build());

		when(marketDataService.searchStocks("茅台")).thenReturn(ApiResponse.success(mockList));

		mockMvc.perform(get("/api/v1/public/market/stocks/search").param("keyword", "茅台")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data[0].name").value("贵州茅台"))
				.andExpect(jsonPath("$.data[0].symbol").value("sh600519"))
				.andExpect(jsonPath("$.data[1].name").value("Apple"))
				.andExpect(jsonPath("$.data[1].symbol").value("gb_aapl"));
	}

	@Test
	void getStockTrend_ReturnsData() throws Exception {
		StockTrendResponse mockTrend = StockTrendResponse.builder().name("贵州茅台").symbol("sh600519")
				.current(new BigDecimal("1610.00")).changePct(new BigDecimal("1.26")).isOpen(true)
				.trendPoints(List.of(new StockTrendResponse.TrendPoint("2026-08-20", new BigDecimal("1590.00")),
						new StockTrendResponse.TrendPoint("2026-08-21", new BigDecimal("1610.00"))))
				.build();

		when(marketDataService.getStockTrend("sh600519", "1M")).thenReturn(ApiResponse.success(mockTrend));

		mockMvc.perform(get("/api/v1/public/market/stocks/sh600519/trend").param("period", "1M")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.name").value("贵州茅台"))
				.andExpect(jsonPath("$.data.current").value(1610.00))
				.andExpect(jsonPath("$.data.trendPoints[0].date").value("2026-08-20"))
				.andExpect(jsonPath("$.data.trendPoints[0].price").value(1590.00));
	}
}
