package space.nebula.nexus.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.response.MarketIndexResponse;
import space.nebula.nexus.service.IMarketDataService;
import space.nebula.nexus.utils.RedisUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataServiceImpl implements IMarketDataService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedisUtil redisUtil;

    private static final String CACHE_KEY = "marketIndices::default";
    // Sina Finance HQ API (Current Prices)
    // s_ prefixes for CN indices return simplified data: name, price, change, changePct, volume, amount
    private static final String SINA_HQ_URL = "http://hq.sinajs.cn/list=gb_ixic,gb_inx,s_sh000001,s_sz399001";
    // Sina Finance K-line API (A-Shares) - scale=60 is 60min, datalen=12 is last 12 bars
    private static final String SINA_KLINE_CN_URL = "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketData.getKLineData?symbol=%s&scale=60&ma=no&datalen=12";
    // Sina Finance K-line API (US Stocks) - type=5 is 5min. We'll take last 12 bars.
    private static final String SINA_KLINE_US_URL = "http://stock.finance.sina.com.cn/usstock/api/json_v2.php/US_MinKService.getMinK?symbol=%s&type=5&___qn=3";

    @Override
    public ApiResponse<List<MarketIndexResponse>> getIndices() {
        // 1. Try to get from cache
        Optional<List> cachedData = redisUtil.get(CACHE_KEY, List.class);
        if (cachedData.isPresent()) {
            return ApiResponse.success((List<MarketIndexResponse>) cachedData.get());
        }

        // 2. Fetch from APIs
        List<MarketIndexResponse> responses = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "http://finance.sina.com.cn");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(SINA_HQ_URL, HttpMethod.GET, entity, byte[].class);
            
            if (response.getBody() == null) {
                return ApiResponse.success(responses);
            }
            
            String body = new String(response.getBody(), "GBK");
            log.debug("Sina HQ response: {}", body);
            
            // US Indices: gb_ixic (Nasdaq), gb_inx (S&P 500)
            MarketIndexResponse nasdaq = parseUsIndex(body, "gb_ixic", "NASDAQ", ".ixic");
            if (nasdaq != null) responses.add(nasdaq);
            
            MarketIndexResponse sp500 = parseUsIndex(body, "gb_inx", "S&P 500", ".inx");
            if (sp500 != null) responses.add(sp500);
            
            // CN Indices: s_sh000001 (SSE), s_sz399001 (SZSE)
            MarketIndexResponse sse = parseCnIndex(body, "s_sh000001", "SSE Composite", "sh000001");
            if (sse != null) responses.add(sse);
            
            MarketIndexResponse szse = parseCnIndex(body, "s_sz399001", "SZSE Component", "sz399001");
            if (szse != null) responses.add(szse);
            
            // 3. Save to cache with 1 minute TTL
            if (!responses.isEmpty()) {
                redisUtil.set(CACHE_KEY, responses, 1, TimeUnit.MINUTES);
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch market indices", e);
        }
        return ApiResponse.success(responses);
    }

    private MarketIndexResponse parseUsIndex(String body, String hqKey, String name, String sparklineSymbol) {
        Pattern pattern = Pattern.compile("var hq_str_" + hqKey + "=\"([^\"]+)\";");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            String[] parts = matcher.group(1).split(",");
            if (parts.length > 2) {
                try {
                    BigDecimal current = new BigDecimal(parts[1]);
                    BigDecimal changePct = new BigDecimal(parts[2]);
                    
                    return MarketIndexResponse.builder()
                            .name(name)
                            .symbol(sparklineSymbol)
                            .current(current.setScale(2, RoundingMode.HALF_UP))
                            .changePct(changePct.setScale(2, RoundingMode.HALF_UP))
                            .sparkline(fetchUsSparkline(sparklineSymbol))
                            .build();
                } catch (Exception e) {
                    log.warn("Failed to parse US index numbers for {}", name);
                }
            }
        }
        return null;
    }

    private MarketIndexResponse parseCnIndex(String body, String hqKey, String name, String sparklineSymbol) {
        Pattern pattern = Pattern.compile("var hq_str_" + hqKey + "=\"([^\"]+)\";");
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            String[] parts = matcher.group(1).split(",");
            if (parts.length > 3) {
                try {
                    // Simple CN HQ: name, current, changeValue, changePct, ...
                    BigDecimal current = new BigDecimal(parts[1]);
                    BigDecimal changePct = new BigDecimal(parts[3]);
                    
                    return MarketIndexResponse.builder()
                            .name(name)
                            .symbol(sparklineSymbol)
                            .current(current.setScale(2, RoundingMode.HALF_UP))
                            .changePct(changePct.setScale(2, RoundingMode.HALF_UP))
                            .sparkline(fetchCnSparkline(sparklineSymbol))
                            .build();
                } catch (Exception e) {
                    log.warn("Failed to parse CN index numbers for {}", name);
                }
            }
        }
        return null;
    }

    private List<BigDecimal> fetchCnSparkline(String symbol) {
        List<BigDecimal> sparkline = new ArrayList<>();
        try {
            String url = String.format(SINA_KLINE_CN_URL, symbol);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://finance.sina.com.cn");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String json = response.getBody();
            
            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        BigDecimal close = new BigDecimal(node.get("close").asText());
                        sparkline.add(close.setScale(2, RoundingMode.HALF_UP));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch CN sparkline for {}", symbol);
        }
        return sparkline;
    }

    private List<BigDecimal> fetchUsSparkline(String symbol) {
        List<BigDecimal> sparkline = new ArrayList<>();
        try {
            String url = String.format(SINA_KLINE_US_URL, symbol);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "https://finance.sina.com.cn");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String json = response.getBody();
            
            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                if (root.isArray()) {
                    // Take the last 12 points for the sparkline
                    int start = Math.max(0, root.size() - 12);
                    for (int i = start; i < root.size(); i++) {
                        BigDecimal close = new BigDecimal(root.get(i).get("c").asText());
                        sparkline.add(close.setScale(2, RoundingMode.HALF_UP));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch US sparkline for {}", symbol);
        }
        return sparkline;
    }
}
