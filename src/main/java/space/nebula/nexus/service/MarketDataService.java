package space.nebula.nexus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import space.nebula.nexus.payload.response.MarketIndexResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Sina Finance HQ API (Current Prices)
    private static final String SINA_HQ_URL = "http://hq.sinajs.cn/list=gb_ixic,gb_inx,sh000001,sz399001";
    // Sina Finance K-line API (A-Shares)
    private static final String SINA_KLINE_CN_URL = "https://quotes.sina.cn/cn/api/json_v2.php/CN_MarketData.getKLineData?symbol=%s&scale=60&ma=no&datalen=12";
    // Sina Finance K-line API (US Stocks)
    private static final String SINA_KLINE_US_URL = "http://stock.finance.sina.com.cn/usstock/api/json_v2.php/US_MinKService.getMinK?symbol=%s&type=60&___qn=3";

    @Cacheable(value = "marketIndices", key = "'default'", unless = "#result == null || #result.isEmpty()")
    public List<MarketIndexResponse> getIndices() {
        List<MarketIndexResponse> responses = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Referer", "http://finance.sina.com.cn");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(SINA_HQ_URL, HttpMethod.GET, entity, byte[].class);
            
            if (response.getBody() == null) {
                return responses;
            }
            
            String body = new String(response.getBody(), "GBK");
            
            MarketIndexResponse nasdaq = parseUsIndex(body, "gb_ixic", "NASDAQ", ".ixic");
            if (nasdaq != null) responses.add(nasdaq);
            
            MarketIndexResponse sp500 = parseUsIndex(body, "gb_inx", "S&P 500", ".inx");
            if (sp500 != null) responses.add(sp500);
            
            MarketIndexResponse sse = parseCnIndex(body, "sh000001", "SSE Composite", "sh000001");
            if (sse != null) responses.add(sse);
            
            MarketIndexResponse szse = parseCnIndex(body, "sz399001", "SZSE Component", "sz399001");
            if (szse != null) responses.add(szse);
            
        } catch (Exception e) {
            log.error("Failed to fetch market indices", e);
        }
        return responses;
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
                            .current(current)
                            .changePct(changePct)
                            .sparkline(fetchUsSparkline(sparklineSymbol))
                            .build();
                } catch (NumberFormatException e) {
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
                    BigDecimal yClose = new BigDecimal(parts[2]);
                    BigDecimal current = new BigDecimal(parts[3]);
                    BigDecimal changePct = BigDecimal.ZERO;
                    if (yClose.compareTo(BigDecimal.ZERO) != 0) {
                        changePct = current.subtract(yClose).divide(yClose, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
                    }
                    
                    return MarketIndexResponse.builder()
                            .name(name)
                            .symbol(sparklineSymbol)
                            .current(current)
                            .changePct(changePct)
                            .sparkline(fetchCnSparkline(sparklineSymbol))
                            .build();
                } catch (NumberFormatException e) {
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
            String json = restTemplate.getForObject(url, String.class);
            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        sparkline.add(new BigDecimal(node.get("close").asText()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch CN sparkline for {}", symbol, e);
        }
        return sparkline;
    }

    private List<BigDecimal> fetchUsSparkline(String symbol) {
        List<BigDecimal> sparkline = new ArrayList<>();
        try {
            String url = String.format(SINA_KLINE_US_URL, symbol);
            String json = restTemplate.getForObject(url, String.class);
            if (json != null) {
                JsonNode root = objectMapper.readTree(json);
                if (root.isArray()) {
                    int start = Math.max(0, root.size() - 12);
                    for (int i = start; i < root.size(); i++) {
                        sparkline.add(new BigDecimal(root.get(i).get("c").asText()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch US sparkline for {}", symbol, e);
        }
        return sparkline;
    }
}
