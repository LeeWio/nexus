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
import space.nebula.nexus.payload.response.MarketIndexResponse;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MarketDataService marketDataService;

    private byte[] mockHqResponse;

    @BeforeEach
    void setUp() throws Exception {
        String hqData = "var hq_str_gb_ixic=\"纳斯达克,15000.50,1.20,2026-05-18 22:01:04,-6.5749,26289.4902,26309.1049,26134.1761,26707.1406,18599.6875,2469627073,7997042033,0,0.00,--,0.00,0.00,0.00,0.00,0,0,0.0000,0.00,0.00,,May 18 10:01AM EDT,26225.1445,0,1,2026,0.0000,0.0000,0.0000,0.0000,0.0000,0.0000\";\n" +
                "var hq_str_gb_inx=\"标普500指数,5000.75,0.80,2026-05-18 22:00:46,11.2600,7418.3901,7434.0601,7394.1099,7517.1201,5767.4102,429201675,3305076297,0,0.00,--,0.00,0.00,0.00,0.00,0,0,0.0000,0.00,0.0000,,May 18 10:00AM EDT,7408.5000,0,1,2026\";\n" +
                "var hq_str_sh000001=\"上证指数,3000.00,2950.00,3050.00,3060.00,2990.00,0,0,627233137,1315084195447,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2026-05-18,15:30:39,00,\";\n" +
                "var hq_str_sz399001=\"深证成指,10000.00,9900.00,10100.00,10200.00,9950.00,0.000,0.000,73830560388,1578895897760.353,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,0,0.000,2026-05-18,15:00:03,00\";";
        mockHqResponse = hqData.getBytes("GBK");
    }

    @Test
    void getIndices_Success() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(mockHqResponse, HttpStatus.OK));

        // Mocking the K-line data calls to return null (so sparklines will be empty, which is fine for this test)
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

        List<MarketIndexResponse> indices = marketDataService.getIndices();

        assertNotNull(indices);
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
        // (3050.00 - 2950.00) / 2950.00 * 100 = 3.3898%
        assertEquals(new BigDecimal("3.3900"), sse.getChangePct());
    }
}
