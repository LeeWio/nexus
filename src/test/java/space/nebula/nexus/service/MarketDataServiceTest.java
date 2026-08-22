package space.nebula.nexus.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.config.MarketProperties;
import space.nebula.nexus.payload.response.MarketIndexResponse;
import space.nebula.nexus.payload.response.StockSearchResponse;
import space.nebula.nexus.payload.response.StockTrendResponse;
import space.nebula.nexus.service.impl.MarketDataServiceImpl;
import space.nebula.nexus.utils.RedisUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

	@Mock
	private RestClient restClient;
	@Mock
	private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
	@Mock
	private RestClient.RequestHeadersSpec requestHeadersSpec;
	@Mock
	private RestClient.ResponseSpec responseSpec;

	@Mock
	private RedisUtil redisUtil;

	@Mock
	private MarketProperties marketProperties;

	@Mock
	private Executor asyncExecutor;

	@InjectMocks
	private MarketDataServiceImpl marketDataService;

	private byte[] mockHqResponse;

	@BeforeEach
	void setUp() throws Exception {
		String hqData = "var hq_str_gb_ixic=\"纳斯达克,15000.50,1.20,2026-05-18 22:01:04,-6.5749,26289.4902,26309.1049,26134.1761,26707.1406,18599.6875,2469627073,7997042033,0,0.00,--,0.00,0.00,0.00,0.00,0,0,0.0000,0.00,0.00,,May 18 10:01AM EDT,26225.1445,0,1,2026,0.0000,0.0000,0.0000,0.0000,0.0000,0.0000\";\n"
				+ "var hq_str_gb_inx=\"标普500指数,5000.75,0.80,2026-05-18 22:00:46,11.2600,7418.3901,7434.0601,7394.1099,7517.1201,5767.4102,429201675,3305076297,0,0.00,--,0.00,0.00,0.00,0.00,0,0,0.0000,0.00,0.0000,,May 18 10:00AM EDT,7408.5000,0,1,2026\";\n"
				+ "var hq_str_s_sh000001=\"上证指数,3050.00,10.00,3.39,627233137,1315084195447\";\n"
				+ "var hq_str_s_sz399001=\"深证成指,10100.00,20.00,2.50,73830560388,1578895897760.353\";";
		mockHqResponse = hqData.getBytes("GBK");

		MarketProperties.IndexConfig ixic = new MarketProperties.IndexConfig();
		ixic.setName("NASDAQ");
		ixic.setSymbol(".ixic");
		ixic.setHqKey("gb_ixic");
		ixic.setType(MarketProperties.MarketType.US);

		MarketProperties.IndexConfig sh = new MarketProperties.IndexConfig();
		sh.setName("SSE Composite");
		sh.setSymbol("sh000001");
		sh.setHqKey("s_sh000001");
		sh.setType(MarketProperties.MarketType.CN);

		MarketProperties.ApiUrls urls = new MarketProperties.ApiUrls();
		urls.setHq("http://hq.sinajs.cn/list=");
		urls.setKlineCn(
				"https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketData.getKLineData?symbol=%s&scale=%s&ma=no&datalen=%d");
		urls.setKlineUs(
				"http://stock.finance.sina.com.cn/usstock/api/json_v2.php/US_MinKService.getMinK?symbol=%s&type=%s&___qn=3");
		urls.setKlineUsDaily(
				"http://stock.finance.sina.com.cn/usstock/api/json_v2.php/US_MinKService.getDailyK?symbol=%s");

		lenient().when(marketProperties.getIndices()).thenReturn(List.of(ixic, sh));
		lenient().when(marketProperties.getUrls()).thenReturn(urls);

		// Mock the async executor to run synchronously for tests
		lenient().doAnswer(invocation -> {
			((Runnable) invocation.getArgument(0)).run();
			return null;
		}).when(asyncExecutor).execute(any(Runnable.class));
	}

	@Test
	void getIndices_Success() {
		when(redisUtil.get(anyString(), eq(List.class))).thenReturn(java.util.Optional.empty());

		// Mock RestClient fluent API
		when(restClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

		// Mock two calls: one for HQ data, others for K-line
		when(responseSpec.body(byte[].class)).thenReturn(mockHqResponse);
		when(responseSpec.body(String.class)).thenReturn("[]");

		ApiResponse<List<MarketIndexResponse>> apiResponse = marketDataService.getIndices("1D");

		assertNotNull(apiResponse);
		assertEquals(200, apiResponse.code());
		List<MarketIndexResponse> indices = apiResponse.data();
		assertEquals(2, indices.size());

		MarketIndexResponse nasdaq = indices.stream().filter(i -> i.getSymbol().equals(".ixic")).findFirst()
				.orElse(null);
		assertNotNull(nasdaq);
		assertEquals("NASDAQ", nasdaq.getName());
		assertEquals(new BigDecimal("15000.50"), nasdaq.getCurrent());

		MarketIndexResponse sse = indices.stream().filter(i -> i.getSymbol().equals("sh000001")).findFirst()
				.orElse(null);
		assertNotNull(sse);
		assertEquals("SSE Composite", sse.getName());
		assertEquals(new BigDecimal("3050.00"), sse.getCurrent());
	}

	@Test
	void refreshIndicesReplacesCacheWithFreshData() {
		when(restClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.body(byte[].class)).thenReturn(mockHqResponse);
		when(responseSpec.body(String.class)).thenReturn("[]");

		int refreshedCount = marketDataService.refreshIndices("1D");

		assertEquals(2, refreshedCount);
		verify(redisUtil).set(eq(CacheConstants.buildFullKey(CacheConstants.MARKET_INDICES, CacheConstants.MARKET_1D)),
				anyList(), eq(1L), eq(java.util.concurrent.TimeUnit.MINUTES));
	}

	@Test
	void searchStocks_Success() throws Exception {
		when(redisUtil.get(anyString(), eq(List.class))).thenReturn(java.util.Optional.empty());

		// Mock RestClient for Suggest API
		when(restClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(contains("suggest"))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

		String mockSuggest = "var suggestdata_123=\"贵州茅台,11,600519,sh600519,贵州茅台,gzmt,贵州茅台,99,1;Apple,41,AAPL,gb_aapl,Apple Inc.,aapl,Apple,0\";";
		when(responseSpec.body(byte[].class)).thenReturn(mockSuggest.getBytes("GBK"));

		ApiResponse<List<StockSearchResponse>> response = marketDataService.searchStocks("茅台");

		assertNotNull(response);
		assertEquals(200, response.code());
		List<StockSearchResponse> results = response.data();
		assertEquals(2, results.size());

		assertEquals("贵州茅台", results.get(0).getName());
		assertEquals("sh600519", results.get(0).getSymbol());
		assertEquals("CN", results.get(0).getMarket());

		assertEquals("Apple Inc.", results.get(1).getName());
		assertEquals("gb_aapl", results.get(1).getSymbol());
		assertEquals("US", results.get(1).getMarket());
	}

	@Test
	void getStockTrend_Success_Cn() throws Exception {
		when(redisUtil.get(anyString(), eq(StockTrendResponse.class))).thenReturn(java.util.Optional.empty());

		// Mock RestClient for CN HQ and CN K-Line
		when(restClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(contains("list=sh600519"))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

		String mockHq = "var hq_str_sh600519=\"贵州茅台,1600.00,1590.00,1610.00,1612.00,1588.00,1610.00,1610.10,...\";";
		when(responseSpec.body(byte[].class)).thenReturn(mockHq.getBytes("GBK"));

		// Mock K-line call
		RestClient.RequestHeadersSpec klineHeaderSpec = mock(RestClient.RequestHeadersSpec.class);
		when(requestHeadersUriSpec.uri(contains("getKLineData"))).thenReturn(klineHeaderSpec);
		when(klineHeaderSpec.header(anyString(), anyString())).thenReturn(klineHeaderSpec);
		when(klineHeaderSpec.retrieve()).thenReturn(responseSpec);

		String mockKline = "[{\"day\":\"2026-08-20 15:00:00\",\"open\":\"1580.00\",\"high\":\"1595.00\",\"low\":\"1575.00\",\"close\":\"1590.00\",\"volume\":\"12345\"},"
				+ "{\"day\":\"2026-08-21 15:00:00\",\"open\":\"1590.00\",\"high\":\"1612.00\",\"low\":\"1588.00\",\"close\":\"1610.00\",\"volume\":\"15000\"}]";
		when(responseSpec.body(String.class)).thenReturn(mockKline);

		ApiResponse<StockTrendResponse> response = marketDataService.getStockTrend("sh600519", "1M");

		assertNotNull(response);
		assertEquals(200, response.code());
		StockTrendResponse trend = response.data();
		assertEquals("贵州茅台", trend.getName());
		assertEquals("sh600519", trend.getSymbol());
		assertEquals(new BigDecimal("1610.00"), trend.getCurrent());
		assertEquals(new BigDecimal("1.26"), trend.getChangePct()); // (1610.00 - 1590.00) / 1590.00 * 100 = 1.2578...
																	// -> 1.26
		assertEquals(2, trend.getTrendPoints().size());
		assertEquals("2026-08-20", trend.getTrendPoints().get(0).getDate());
		assertEquals(new BigDecimal("1590.00"), trend.getTrendPoints().get(0).getPrice());
	}

	@Test
	void getStockTrend_Success_Us() throws Exception {
		when(redisUtil.get(anyString(), eq(StockTrendResponse.class))).thenReturn(java.util.Optional.empty());

		// Mock RestClient for US HQ and US K-Line
		when(restClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(contains("list=gb_aapl"))).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
		when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

		String mockHq = "var hq_str_gb_aapl=\"苹果,175.50,-1.25,2026-08-21 16:00:00,...\";";
		when(responseSpec.body(byte[].class)).thenReturn(mockHq.getBytes("GBK"));

		// Mock K-line call
		RestClient.RequestHeadersSpec klineHeaderSpec = mock(RestClient.RequestHeadersSpec.class);
		when(requestHeadersUriSpec.uri(contains("getDailyK"))).thenReturn(klineHeaderSpec);
		when(klineHeaderSpec.header(anyString(), anyString())).thenReturn(klineHeaderSpec);
		when(klineHeaderSpec.retrieve()).thenReturn(responseSpec);

		String mockKline = "[{\"d\":\"2026-08-20\",\"c\":\"176.00\"},{\"d\":\"2026-08-21\",\"c\":\"175.50\"}]";
		when(responseSpec.body(String.class)).thenReturn(mockKline);

		ApiResponse<StockTrendResponse> response = marketDataService.getStockTrend("AAPL", "1M");

		assertNotNull(response);
		assertEquals(200, response.code());
		StockTrendResponse trend = response.data();
		assertEquals("苹果", trend.getName());
		assertEquals("aapl", trend.getSymbol());
		assertEquals(new BigDecimal("175.50"), trend.getCurrent());
		assertEquals(new BigDecimal("-1.25"), trend.getChangePct());
		assertEquals(2, trend.getTrendPoints().size());
		assertEquals("2026-08-20", trend.getTrendPoints().get(0).getDate());
		assertEquals(new BigDecimal("176.00"), trend.getTrendPoints().get(0).getPrice());
	}
}
