package space.nebula.nexus.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.config.MarketProperties;
import space.nebula.nexus.payload.response.MarketIndexResponse;
import space.nebula.nexus.payload.response.StockSearchResponse;
import space.nebula.nexus.payload.response.StockTrendResponse;
import space.nebula.nexus.service.IMarketDataService;
import space.nebula.nexus.utils.RedisUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MarketDataServiceImpl implements IMarketDataService {

	private final RestClient restClient;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RedisUtil redisUtil;
	private final MarketProperties marketProperties;
	private final Executor outboundExecutor;

	@Override
	@CircuitBreaker(name = "marketService", fallbackMethod = "fallbackIndices")
	@Retry(name = "marketService")
	public ApiResponse<List<MarketIndexResponse>> getIndices(String period) {
		String normalizedPeriod = normalizePeriod(period);

		String cacheKey = CacheConstants.buildFullKey(CacheConstants.MARKET_INDICES, normalizedPeriod);
		Optional<List> cachedData = redisUtil.get(cacheKey, List.class);
		if (cachedData.isPresent()) {
			return ApiResponse.success((List<MarketIndexResponse>) cachedData.get());
		}

		List<MarketIndexResponse> responses = fetchIndicesFromApi(normalizedPeriod);

		if (!responses.isEmpty()) {
			cacheIndices(normalizedPeriod, responses);
		}

		return ApiResponse.success(responses);
	}

	private List<MarketIndexResponse> fetchIndicesFromApi(String normalizedPeriod) {
		if (marketProperties.getIndices() == null || marketProperties.getIndices().isEmpty()) {
			return new ArrayList<>();
		}

		String hqKeys = marketProperties.getIndices().stream().map(MarketProperties.IndexConfig::getHqKey)
				.collect(Collectors.joining(","));

		String hqUrl = marketProperties.getUrls().getHq() + hqKeys;

		byte[] responseBytes = restClient.get().uri(hqUrl).header("Referer", "http://finance.sina.com.cn").header(
				"User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
				.retrieve().body(byte[].class);

		if (responseBytes == null) {
			return new ArrayList<>();
		}

		String body = new String(responseBytes, Charset.forName("GBK"));

		List<CompletableFuture<MarketIndexResponse>> futures = marketProperties.getIndices().stream()
				.map(config -> CompletableFuture.supplyAsync(() -> {
					if (config.getType() == MarketProperties.MarketType.US) {
						return parseUsIndex(body, config, normalizedPeriod);
					} else {
						return parseCnIndex(body, config, normalizedPeriod);
					}
				}, outboundExecutor)).collect(Collectors.toList());

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		return futures.stream().map(CompletableFuture::join).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	public ApiResponse<MarketIndexResponse> getIndex(String symbol, String period) {
		ApiResponse<List<MarketIndexResponse>> allIndicesResponse = getIndices(period);
		if (allIndicesResponse.code() == 200 && allIndicesResponse.data() != null) {
			for (MarketIndexResponse index : allIndicesResponse.data()) {
				if (index.getSymbol().equalsIgnoreCase(symbol)) {
					return ApiResponse.success(index);
				}
			}
		}
		return ApiResponse.error(404, "Market index not found for symbol: " + symbol);
	}

	@Override
	@CircuitBreaker(name = "marketService", fallbackMethod = "fallbackRefreshIndices")
	@Retry(name = "marketService")
	public int refreshIndices(String period) {
		String normalizedPeriod = normalizePeriod(period);
		List<MarketIndexResponse> responses = fetchIndicesFromApi(normalizedPeriod);
		if (responses.isEmpty()) {
			return 0;
		}

		cacheIndices(normalizedPeriod, responses);
		return responses.size();
	}

	/**
	 * Returns a safe refresh result when the upstream market provider is
	 * unavailable.
	 *
	 * @param period
	 *            requested market data period
	 * @param exception
	 *            upstream failure that triggered the fallback
	 * @return zero because no cache entries were refreshed
	 */
	public int fallbackRefreshIndices(String period, Exception exception) {
		log.error("Market cache refresh fallback triggered for period {}: {}", period, exception.getMessage());
		return 0;
	}

	private String normalizePeriod(String period) {
		String normalizedPeriod = period != null ? period.toUpperCase() : CacheConstants.MARKET_1D;
		return List.of("1D", "1M", "1Y", "ALL").contains(normalizedPeriod)
				? normalizedPeriod
				: CacheConstants.MARKET_1D;
	}

	private long cacheTtl(String period) {
		if ("1M".equals(period)) {
			return 30;
		}
		return "1Y".equals(period) || "ALL".equals(period) ? 6 : 1;
	}

	private TimeUnit cacheTtlUnit(String period) {
		return "1Y".equals(period) || "ALL".equals(period) ? TimeUnit.HOURS : TimeUnit.MINUTES;
	}

	private void cacheIndices(String period, List<MarketIndexResponse> responses) {
		String cacheKey = CacheConstants.buildFullKey(CacheConstants.MARKET_INDICES, period);
		redisUtil.set(cacheKey, responses, cacheTtl(period), cacheTtlUnit(period));
	}

	public ApiResponse<List<MarketIndexResponse>> fallbackIndices(String period, Exception e) {
		log.error("Market indices fallback triggered due to: {}", e.getMessage());
		return ApiResponse.success(new ArrayList<>());
	}

	private MarketIndexResponse parseUsIndex(String body, MarketProperties.IndexConfig config, String period) {
		Pattern pattern = Pattern.compile("var hq_str_" + config.getHqKey() + "=\"([^\"]+)\";");
		Matcher matcher = pattern.matcher(body);
		if (matcher.find()) {
			String[] parts = matcher.group(1).split(",");
			if (parts.length > 2) {
				try {
					BigDecimal current = new BigDecimal(parts[1]);
					List<BigDecimal> sparkline = fetchUsSparkline(config.getSymbol(), period);

					BigDecimal changePct;
					if ("1D".equalsIgnoreCase(period)) {
						changePct = new BigDecimal(parts[2]);
					} else if (CollUtil.isNotEmpty(sparkline)) {
						BigDecimal startPrice = sparkline.get(0);
						if (startPrice.compareTo(BigDecimal.ZERO) != 0) {
							changePct = current.subtract(startPrice).divide(startPrice, 4, RoundingMode.HALF_UP)
									.multiply(new BigDecimal("100"));
						} else {
							changePct = BigDecimal.ZERO;
						}
					} else {
						changePct = BigDecimal.ZERO;
					}

					return MarketIndexResponse.builder().name(config.getName()).symbol(config.getSymbol())
							.current(current.setScale(2, RoundingMode.HALF_UP))
							.changePct(changePct.setScale(2, RoundingMode.HALF_UP)).sparkline(sparkline)
							.isOpen(isUsMarketOpen()).build();
				} catch (Exception e) {
					log.warn("Failed to parse US index numbers for {}", config.getName());
				}
			}
		}
		return null;
	}

	private MarketIndexResponse parseCnIndex(String body, MarketProperties.IndexConfig config, String period) {
		Pattern pattern = Pattern.compile("var hq_str_" + config.getHqKey() + "=\"([^\"]+)\";");
		Matcher matcher = pattern.matcher(body);
		if (matcher.find()) {
			String[] parts = matcher.group(1).split(",");
			if (parts.length > 3) {
				try {
					BigDecimal current = new BigDecimal(parts[1]);
					List<BigDecimal> sparkline = fetchCnSparkline(config.getSymbol(), period);

					BigDecimal changePct;
					if ("1D".equalsIgnoreCase(period)) {
						changePct = new BigDecimal(parts[3]);
					} else if (CollUtil.isNotEmpty(sparkline)) {
						BigDecimal startPrice = sparkline.get(0);
						if (startPrice.compareTo(BigDecimal.ZERO) != 0) {
							changePct = current.subtract(startPrice).divide(startPrice, 4, RoundingMode.HALF_UP)
									.multiply(new BigDecimal("100"));
						} else {
							changePct = BigDecimal.ZERO;
						}
					} else {
						changePct = BigDecimal.ZERO;
					}

					return MarketIndexResponse.builder().name(config.getName()).symbol(config.getSymbol())
							.current(current.setScale(2, RoundingMode.HALF_UP))
							.changePct(changePct.setScale(2, RoundingMode.HALF_UP)).sparkline(sparkline)
							.isOpen(isCnMarketOpen()).build();
				} catch (Exception e) {
					log.warn("Failed to parse CN index numbers for {}", config.getName());
				}
			}
		}
		return null;
	}

	private List<BigDecimal> fetchCnSparkline(String symbol, String period) {
		List<BigDecimal> sparkline = new ArrayList<>();
		String scale = "5";
		int datalen = 48;

		switch (period) {
			case "1M" :
				scale = "240";
				datalen = 22;
				break;
			case "1Y" :
				scale = "240";
				datalen = 250;
				break;
			case "ALL" :
				scale = "240";
				datalen = 1000;
				break;
			case "1D" :
			default :
				scale = "5";
				datalen = 48;
				break;
		}

		try {
			String url = String.format(marketProperties.getUrls().getKlineCn(), symbol, scale, datalen);
			String json = restClient.get().uri(url).header("Referer", "https://finance.sina.com.cn").retrieve()
					.body(String.class);

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

	private List<BigDecimal> fetchUsSparkline(String symbol, String period) {
		List<BigDecimal> sparkline = new ArrayList<>();
		String type = "5";
		int datalen = 78;
		boolean isDaily = false;

		switch (period) {
			case "1M" :
				isDaily = true;
				datalen = 22;
				break;
			case "1Y" :
				isDaily = true;
				datalen = 250;
				break;
			case "ALL" :
				isDaily = true;
				datalen = 1000;
				break;
			case "1D" :
			default :
				type = "5";
				datalen = 78;
				break;
		}

		try {
			String url;
			if (isDaily) {
				url = String.format(marketProperties.getUrls().getKlineUsDaily(), symbol);
			} else {
				url = String.format(marketProperties.getUrls().getKlineUs(), symbol, type);
			}

			String json = restClient.get().uri(url).header("Referer", "https://finance.sina.com.cn").retrieve()
					.body(String.class);

			if (json != null) {
				JsonNode root = objectMapper.readTree(json);
				if (root.isArray()) {
					int start = Math.max(0, root.size() - datalen);
					for (int i = start; i < root.size(); i++) {
						JsonNode node = root.get(i);
						if (node.has("c")) {
							BigDecimal close = new BigDecimal(node.get("c").asText());
							sparkline.add(close.setScale(2, RoundingMode.HALF_UP));
						}
					}
				}
			}
		} catch (Exception e) {
			log.warn("Failed to fetch US sparkline for {}", symbol);
		}
		return sparkline;
	}

	private boolean isUsMarketOpen() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/New_York"));
		DayOfWeek day = now.getDayOfWeek();
		if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
			return false;
		}
		LocalTime time = now.toLocalTime();
		LocalTime openTime = LocalTime.of(9, 30);
		LocalTime closeTime = LocalTime.of(16, 0);
		return !time.isBefore(openTime) && !time.isAfter(closeTime);
	}

	private boolean isCnMarketOpen() {
		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
		DayOfWeek day = now.getDayOfWeek();
		if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
			return false;
		}
		LocalTime time = now.toLocalTime();
		LocalTime amOpen = LocalTime.of(9, 30);
		LocalTime amClose = LocalTime.of(11, 30);
		LocalTime pmOpen = LocalTime.of(13, 0);
		LocalTime pmClose = LocalTime.of(15, 0);

		boolean isAmOpen = !time.isBefore(amOpen) && !time.isAfter(amClose);
		boolean isPmOpen = !time.isBefore(pmOpen) && !time.isAfter(pmClose);
		return isAmOpen || isPmOpen;
	}

	@Override
	public ApiResponse<List<StockSearchResponse>> searchStocks(String keyword) {
		if (StrUtil.isBlank(keyword)) {
			return ApiResponse.success(new ArrayList<>());
		}

		String cleanKeyword = keyword.trim().toLowerCase();
		String cacheKey = CacheConstants.buildFullKey("stockSearch", cleanKeyword);
		Optional<List> cachedData = redisUtil.get(cacheKey, List.class);
		if (cachedData.isPresent()) {
			return ApiResponse.success((List<StockSearchResponse>) cachedData.get());
		}

		List<StockSearchResponse> results = new ArrayList<>();
		try {
			// Query Sina Suggest API
			String url = "http://suggest3.sinajs.cn/suggest/key=" + cleanKeyword;
			byte[] responseBytes = restClient.get().uri(url)
					.header("Referer", "http://finance.sina.com.cn")
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.retrieve().body(byte[].class);

			if (responseBytes != null) {
				String body = new String(responseBytes, Charset.forName("GBK"));
				Pattern pattern = Pattern.compile("\"([^\"]*)\"");
				Matcher matcher = pattern.matcher(body);
				if (matcher.find()) {
					String rawResults = matcher.group(1);
					if (StrUtil.isNotBlank(rawResults)) {
						String[] items = rawResults.split(";");
						for (String item : items) {
							String[] fields = item.split(",");
							if (fields.length >= 6) {
								String marketType = fields[1];
								String market = null;
								if ("11".equals(marketType) || "111".equals(marketType) || "12".equals(marketType)) {
									market = "CN";
								} else if ("41".equals(marketType)) {
									market = "US";
								}

								if (market != null) {
									results.add(StockSearchResponse.builder()
											.name(fields[4])
											.code(fields[2])
											.symbol(fields[3])
											.market(market)
											.build());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Failed to search stocks for keyword: {}", keyword, e);
			return ApiResponse.error(500, "Failed to search stocks due to upstream/network issue: " + e.getMessage());
		}

		if (!results.isEmpty()) {
			redisUtil.set(cacheKey, results, 10, TimeUnit.MINUTES);
		}
		return ApiResponse.success(results);
	}

	@Override
	public ApiResponse<StockTrendResponse> getStockTrend(String symbol, String period) {
		if (StrUtil.isBlank(symbol)) {
			return ApiResponse.error(400, "Stock symbol must not be empty");
		}

		String cleanPeriod = period != null ? period.toUpperCase() : "1M";
		if (!List.of("1W", "1M", "1Y").contains(cleanPeriod)) {
			cleanPeriod = "1M";
		}

		String cleanSymbol = symbol.toLowerCase().trim();
		String cacheKey = CacheConstants.buildFullKey("stockTrend", cleanSymbol + ":" + cleanPeriod);
		Optional<StockTrendResponse> cachedData = redisUtil.get(cacheKey, StockTrendResponse.class);
		if (cachedData.isPresent()) {
			return ApiResponse.success(cachedData.get());
		}

		boolean isCn = (cleanSymbol.startsWith("sh") || cleanSymbol.startsWith("sz"))
				&& cleanSymbol.length() > 2
				&& Character.isDigit(cleanSymbol.charAt(2));

		// Get real-time stock details from Sina HQ
		String hqKey;
		if (isCn) {
			hqKey = cleanSymbol;
		} else {
			hqKey = cleanSymbol.startsWith("gb_") ? cleanSymbol : "gb_" + cleanSymbol;
		}

		String hqUrl = marketProperties.getUrls().getHq() + hqKey;
		String name = null;
		BigDecimal currentPrice = BigDecimal.ZERO;
		BigDecimal changePct = BigDecimal.ZERO;

		try {
			byte[] responseBytes = restClient.get().uri(hqUrl)
					.header("Referer", "http://finance.sina.com.cn")
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
					.retrieve().body(byte[].class);

			if (responseBytes == null) {
				return ApiResponse.error(404, "Failed to fetch stock info for " + symbol);
			}

			String hqBody = new String(responseBytes, Charset.forName("GBK"));
			Pattern pattern = Pattern.compile("var hq_str_" + hqKey + "=\"([^\"]+)\";");
			Matcher matcher = pattern.matcher(hqBody);

			if (matcher.find()) {
				String[] parts = matcher.group(1).split(",");
				if (isCn && parts.length > 3) {
					name = parts[0];
					currentPrice = new BigDecimal(parts[3]);
					BigDecimal yesterdayClose = new BigDecimal(parts[2]);
					if (yesterdayClose.compareTo(BigDecimal.ZERO) != 0) {
						changePct = currentPrice.subtract(yesterdayClose)
								.divide(yesterdayClose, 4, RoundingMode.HALF_UP)
								.multiply(new BigDecimal("100"))
								.setScale(2, RoundingMode.HALF_UP);
					}
				} else if (!isCn && parts.length > 2) {
					name = parts[0];
					currentPrice = new BigDecimal(parts[1]);
					changePct = new BigDecimal(parts[2]).setScale(2, RoundingMode.HALF_UP);
				}
			}
		} catch (Exception e) {
			log.error("Failed to fetch real-time quote for symbol: {}", symbol, e);
			return ApiResponse.error(500, "Failed to fetch real-time quote for " + symbol);
		}

		if (name == null) {
			return ApiResponse.error(404, "Stock data not found or parse failed for " + symbol);
		}

		// Fetch historical K-line points
		List<StockTrendResponse.TrendPoint> trendPoints = new ArrayList<>();
		if (isCn) {
			String scale = "240"; // Daily
			int datalen = 22;

			switch (cleanPeriod) {
				case "1W":
					scale = "240";
					datalen = 5;
					break;
				case "1Y":
					scale = "240";
					datalen = 250;
					break;
				case "1M":
				default:
					scale = "240";
					datalen = 22;
					break;
			}

			try {
				String url = String.format(marketProperties.getUrls().getKlineCn(), cleanSymbol, scale, datalen);
				String json = restClient.get().uri(url)
						.header("Referer", "https://finance.sina.com.cn")
						.retrieve().body(String.class);

				if (json != null) {
					JsonNode root = objectMapper.readTree(json);
					if (root.isArray()) {
						for (JsonNode node : root) {
							String date = node.get("day").asText();
							if (date.contains(" ")) {
								date = date.split(" ")[0];
							}
							BigDecimal close = new BigDecimal(node.get("close").asText());
							trendPoints.add(new StockTrendResponse.TrendPoint(date, close.setScale(2, RoundingMode.HALF_UP)));
						}
					}
				}
			} catch (Exception e) {
				log.warn("Failed to fetch CN trend for {}", symbol, e);
			}
		} else {
			int datalen = 22;
			switch (cleanPeriod) {
				case "1W":
					datalen = 5;
					break;
				case "1Y":
					datalen = 250;
					break;
				case "1M":
				default:
					datalen = 22;
					break;
			}

			try {
				String ticker = cleanSymbol.startsWith("gb_") ? cleanSymbol.substring(3) : cleanSymbol;
				String url = String.format(marketProperties.getUrls().getKlineUsDaily(), ticker);
				String json = restClient.get().uri(url)
						.header("Referer", "https://finance.sina.com.cn")
						.retrieve().body(String.class);

				if (json != null) {
					JsonNode root = objectMapper.readTree(json);
					if (root.isArray()) {
						int start = Math.max(0, root.size() - datalen);
						for (int i = start; i < root.size(); i++) {
							JsonNode node = root.get(i);
							if (node.has("c") && node.has("d")) {
								String date = node.get("d").asText();
								BigDecimal close = new BigDecimal(node.get("c").asText());
								trendPoints.add(new StockTrendResponse.TrendPoint(date, close.setScale(2, RoundingMode.HALF_UP)));
							}
						}
					}
				}
			} catch (Exception e) {
				log.warn("Failed to fetch US trend for {}", symbol, e);
			}
		}

		boolean isOpen = isCn ? isCnMarketOpen() : isUsMarketOpen();

		StockTrendResponse response = StockTrendResponse.builder()
				.name(name)
				.symbol(cleanSymbol)
				.current(currentPrice.setScale(2, RoundingMode.HALF_UP))
				.changePct(changePct)
				.isOpen(isOpen)
				.trendPoints(trendPoints)
				.build();

		redisUtil.set(cacheKey, response, getStockTrendCacheTtl(cleanPeriod), getStockTrendCacheTtlUnit(cleanPeriod));
		return ApiResponse.success(response);
	}

	private long getStockTrendCacheTtl(String period) {
		switch (period.toUpperCase()) {
			case "1W":
				return 5;
			case "1Y":
				return 4;
			case "1M":
			default:
				return 30;
		}
	}

	private TimeUnit getStockTrendCacheTtlUnit(String period) {
		if ("1Y".equalsIgnoreCase(period)) {
			return TimeUnit.HOURS;
		}
		return TimeUnit.MINUTES;
	}
}
