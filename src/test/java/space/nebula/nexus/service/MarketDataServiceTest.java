package space.nebula.nexus.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.config.MarketProperties;
import space.nebula.nexus.payload.response.MarketIndexResponse;
import space.nebula.nexus.service.impl.MarketDataServiceImpl;
import space.nebula.nexus.utils.RedisUtil;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private MarketProperties marketProperties;

    @InjectMocks
    private MarketDataServiceImpl marketDataService;

    private byte[] mockHqResponse;

    @BeforeEach
    void setUp() throws Exception {
        String hqData = "var hq_str_gb_ixic=\"纳斯达克,15000.50,1.20,2026-05-18 22:01:04,-6.5749,26289.4902,26309.1049,26134.1761,26707.1406,18599.6875,2469627073,7997042033,0,0.00,--,0.00,0.00,0.00,0.00,0,0,0.0000,0.00,0.00,,May 18 10:01AM EDT,26225.1445,0,1,2026,0.0000,0.0000,0.0000,0.0000,0.0000,0.0000\";\n" +
                "var hq_str_gb_inx=\"标普500指数,5000.75,0.80,2026-05-18 22:00:46,11.2600,7418.3901,7434.0601,7394.1099,7517.1201,5767.4102,429201675,3305076297,0,0.00,--,0.00,0.00,0.00,0.00,0,0,0.0000,0.00,0.0000,,May 18 10:00AM EDT,7408.5000,0,1,2026\";\n" +
                "var hq_str_s_sh000001=\"上证指数,3050.00,10.00,3.39,627233137,1315084195447\";\n" +
                "var hq_str_s_sz399001=\"深证成指,10100.00,20.00,2.50,73830560388,1578895897760.353\";";
        mockHqResponse = hqData.getBytes("GBK");
        
        MarketProperties.IndexConfig ixic = new MarketProperties.IndexConfig();
        ixic.setName("NASDAQ"); ixic.setSymbol(".ixic"); ixic.setHqKey("gb_ixic"); ixic.setType(MarketProperties.MarketType.US);
        
        MarketProperties.IndexConfig inx = new MarketProperties.IndexConfig();
        inx.setName("S&P 500"); inx.setSymbol(".inx"); inx.setHqKey("gb_inx"); inx.setType(MarketProperties.MarketType.US);
        
        MarketProperties.IndexConfig sh = new MarketProperties.IndexConfig();
        sh.setName("SSE Composite"); sh.setSymbol("sh000001"); sh.setHqKey("s_sh000001"); sh.setType(MarketProperties.MarketType.CN);
        
        MarketProperties.IndexConfig sz = new MarketProperties.IndexConfig();
        sz.setName("SZSE Component"); sz.setSymbol("sz399001"); sz.setHqKey("s_sz399001"); sz.setType(MarketProperties.MarketType.CN);
        
        MarketProperties.ApiUrls urls = new MarketProperties.ApiUrls();
        urls.setHq("http://hq.sinajs.cn/list=");
        urls.setKlineCn("https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketData.getKLineData?symbol=%s&scale=%s&ma=no&datalen=%d");
        urls.setKlineUs("http://stock.finance.sina.com.cn/usstock/api/json_v2.php/US_MinKService.getMinK?symbol=%s&type=%s&___qn=3");
        urls.setKlineUsDaily("http://stock.finance.sina.com.cn/usstock/api/json_v2.php/US_MinKService.getDailyK?symbol=%s");

        lenient().when(marketProperties.getIndices()).thenReturn(List.of(ixic, inx, sh, sz));
        lenient().when(marketProperties.getUrls()).thenReturn(urls);
    }

    @Test
    void getIndices_Success() {
        when(redisUtil.get(anyString(), eq(List.class))).thenReturn(java.util.Optional.empty());

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(mockHqResponse, HttpStatus.OK));

        // Mocking the K-line data calls to return empty array
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>("[]", HttpStatus.OK));

        ApiResponse<List<MarketIndexResponse>> apiResponse = marketDataService.getIndices("1D");

        assertNotNull(apiResponse);
        assertEquals(200, apiResponse.getCode());
        List<MarketIndexResponse> indices = apiResponse.getData();
        assertEquals(4, indices.size());

        MarketIndexResponse nasdaq = indices.stream().filter(i -> i.getSymbol().equals(".ixic")).findFirst().orElse(null);
        assertNotNull(nasdaq);
        assertEquals("NASDAQ", nasdaq.getName());
        assertEquals(new BigDecimal("15000.50"), nasdaq.getCurrent());
        assertEquals(new BigDecimal("1.20"), nasdaq.getChangePct());

        MarketIndexResponse sse = indices.stream().filter(i -> i.getSymbol().equals("sh000001")).findFirst().orElse(null);
        assertNotNull(sse);
        assertEquals("SSE Composite", sse.getName());
        assertEquals(new BigDecimal("3050.00"), sse.getCurrent());
        assertEquals(new BigDecimal("3.39"), sse.getChangePct());
    }
}
