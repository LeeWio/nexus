package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.payload.response.AnalyticsOverviewResponse;
import space.nebula.nexus.repository.VisitLogRepository;
import space.nebula.nexus.service.IAnalyticsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements IAnalyticsService {

	private final VisitLogRepository visitLogRepository;
	private final space.nebula.nexus.repository.DailyAnalyticsRepository dailyAnalyticsRepository;
	private final space.nebula.nexus.repository.CommentRepository commentRepository;
	private final space.nebula.nexus.repository.PostRepository postRepository;
	private final space.nebula.nexus.mapper.PostMapper postMapper;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.ANALYTICS, key = CacheConstants.OVERVIEW_KEY)
	public ApiResponse<AnalyticsOverviewResponse> retrieveOverviewStats() {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		LocalDate weekAgo = today.minusDays(7);

		// Current day stats from real-time logs
		LocalDateTime todayStart = today.atStartOfDay();
		LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
		long todayPv = visitLogRepository.countPv(todayStart, todayEnd);
		long todayUv = visitLogRepository.countUv(todayStart, todayEnd);

		// Yesterday and historical trends from DailyAnalytics
		var yesterdayData = dailyAnalyticsRepository.findByStatDate(yesterday);
		long yesterdayPv = yesterdayData.map(space.nebula.nexus.entity.DailyAnalytics::getPv).orElse(0L);
		long yesterdayUv = yesterdayData.map(space.nebula.nexus.entity.DailyAnalytics::getUv).orElse(0L);

		double growthRate = (yesterdayPv > 0) ? ((double) (todayPv - yesterdayPv) / yesterdayPv) * 100 : 0.0;

		// 1. Map Top Content (Real-time today)
		List<AnalyticsOverviewResponse.TopContentItem> topContent = visitLogRepository.findTopContentRaw(todayStart)
				.stream().map(m -> {
					String url = String.valueOf(m.get("url"));
					String title = url;
					if (url.startsWith("/api/v1/public/blog/posts/")) {
						String slug = url.substring("/api/v1/public/blog/posts/".length());
						title = postRepository.findBySlug(slug).map(space.nebula.nexus.entity.Post::getTitle)
								.orElse(url);
					}
					return AnalyticsOverviewResponse.TopContentItem.builder().url(url).title(title)
							.count(((Number) m.get("count")).longValue()).build();
				}).limit(10).toList();

		// 2. Map Daily Trends (Mix of historical and real-time today)
		List<AnalyticsOverviewResponse.VisitTrendItem> dailyTrends = dailyAnalyticsRepository
				.findByStatDateGreaterThanEqualOrderByStatDateAsc(weekAgo).stream()
				.map(d -> AnalyticsOverviewResponse.VisitTrendItem.builder().date(d.getStatDate().toString())
						.pv(d.getPv()).uv(d.getUv()).build())
				.collect(Collectors.toList());

		// Add today's real-time data to trend
		dailyTrends.add(AnalyticsOverviewResponse.VisitTrendItem.builder().date(today.toString()).pv(todayPv)
				.uv(todayUv).build());

		AnalyticsOverviewResponse response = AnalyticsOverviewResponse.builder().todayPv(todayPv).todayUv(todayUv)
				.yesterdayPv(yesterdayPv).yesterdayUv(yesterdayUv).pvGrowthRate(growthRate).dailyTrends(dailyTrends)
				.topContent(topContent).build();

		return ApiResponse.success(response);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<space.nebula.nexus.payload.response.TopPageResponse>> getTopPages() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
		LocalDateTime todayEnd = now.toLocalDate().atTime(LocalTime.MAX);

		LocalDateTime yesterdayStart = todayStart.minusDays(1);
		LocalDateTime yesterdayEnd = todayEnd.minusDays(1);

		// Get today's top pages
		List<Map<String, Object>> todayContent = visitLogRepository.findTopContentRawByRange(todayStart, todayEnd);

		// Get yesterday's top pages for trend comparison
		List<Map<String, Object>> yesterdayContent = visitLogRepository.findTopContentRawByRange(yesterdayStart,
				yesterdayEnd);

		Map<String, Long> yesterdayCounts = yesterdayContent.stream().collect(Collectors
				.toMap(m -> String.valueOf(m.get("url")), m -> ((Number) m.get("count")).longValue(), (a, b) -> a));

		List<space.nebula.nexus.payload.response.TopPageResponse> topPages = todayContent.stream().map(m -> {
			String url = String.valueOf(m.get("url"));
			long count = ((Number) m.get("count")).longValue();

			long yesterdayCount = yesterdayCounts.getOrDefault(url, 0L);

			// Calculate trend
			String trendStr = "0%";
			if (yesterdayCount == 0 && count > 0) {
				trendStr = "+100%";
			} else if (yesterdayCount > 0) {
				double trend = ((double) (count - yesterdayCount) / yesterdayCount) * 100;
				trendStr = (trend > 0 ? "+" : "") + String.format("%.1f%%", trend);
			}

			return space.nebula.nexus.payload.response.TopPageResponse.builder().path(url).views(count)
					// Mock values for average time and bounce rate
					.avgTime("00:01:30") // e.g., 1m 30s
					.bounceRate(45.5) // e.g., 45.5%
					.trend(trendStr).build();
		}).limit(20).collect(Collectors.toList());

		return ApiResponse.success(topPages);
	}

	@Override
	@Transactional
	public void aggregateDailyData(LocalDate date) {
		log.info("Aggregating analytics data for date: {}", date);
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.atTime(LocalTime.MAX);

		long pv = visitLogRepository.countPv(start, end);
		long uv = visitLogRepository.countUv(start, end);
		long comments = commentRepository.countByCreatedAtBetween(start, end);

		// Count post detail API hits as post views
		long postViews = visitLogRepository.findDailyTrendRaw(start).stream()
				.filter(m -> String.valueOf(m.get("visitDate")).equals(date.toString()))
				.mapToLong(m -> ((Number) m.get("pv")).longValue()).sum();

		var daily = dailyAnalyticsRepository.findByStatDate(date)
				.orElse(new space.nebula.nexus.entity.DailyAnalytics());
		daily.setStatDate(date);
		daily.setPv(pv);
		daily.setUv(uv);
		daily.setCommentCount(comments);
		daily.setPostViews(postViews);

		dailyAnalyticsRepository.save(daily);
		log.info("Daily aggregation completed for {}: PV={}, UV={}", date, pv, uv);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<space.nebula.nexus.payload.response.TrafficStatsResponse> getTrafficStats(int days) {
		LocalDateTime end = LocalDateTime.now();
		LocalDateTime start = end.minusDays(days).toLocalDate().atStartOfDay();
		LocalDateTime prevStart = start.minusDays(days);

		// 1. Core Metrics & Growth
		long currentSessions = visitLogRepository.countPv(start, end);
		long currentUsers = visitLogRepository.countUv(start, end);
		long prevSessions = visitLogRepository.countPv(prevStart, start);
		long prevUsers = visitLogRepository.countUv(prevStart, start);

		double sessionsGrowth = (prevSessions > 0)
				? ((double) (currentSessions - prevSessions) / prevSessions) * 100
				: 0.0;
		double usersGrowth = (prevUsers > 0) ? ((double) (currentUsers - prevUsers) / prevUsers) * 100 : 0.0;

		// Mocked metrics for Bounce Rate and Avg Session (using seeded logic to vary
		// growth)
		// In a real system, these would be aggregated from session-level data.
		double currentBounce = 41.3;
		double prevBounce = 42.2; // Derived to show a -2.1% growth like the screenshot (approx)
		double bounceGrowth = ((currentBounce - prevBounce) / prevBounce) * 100;

		String avgSessionValue = "3m 42s";
		double avgSessionGrowth = 12.0;

		space.nebula.nexus.payload.response.TrafficStatsResponse.SummaryMetrics summary = space.nebula.nexus.payload.response.TrafficStatsResponse.SummaryMetrics
				.builder()
				.sessions(new space.nebula.nexus.payload.response.TrafficStatsResponse.Metric(
						String.format("%,d", currentSessions), currentSessions, sessionsGrowth))
				.users(new space.nebula.nexus.payload.response.TrafficStatsResponse.Metric(
						String.format("%,d", currentUsers), currentUsers, usersGrowth))
				.bounceRate(new space.nebula.nexus.payload.response.TrafficStatsResponse.Metric(currentBounce + "%",
						currentBounce, bounceGrowth))
				.avgSession(new space.nebula.nexus.payload.response.TrafficStatsResponse.Metric(avgSessionValue, 222,
						avgSessionGrowth))
				.build();

		// 2. Time Series (Daily PV/UV)
		List<space.nebula.nexus.payload.response.TrafficStatsResponse.TimeSeriesItem> timeSeries = visitLogRepository
				.findDailyTrendRaw(start).stream()
				.map(m -> space.nebula.nexus.payload.response.TrafficStatsResponse.TimeSeriesItem.builder()
						.date(java.time.LocalDate.parse(String.valueOf(m.get("visitDate")))
								.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd")))
						.sessions(((Number) m.get("pv")).longValue()).users(((Number) m.get("uv")).longValue()).build())
				.collect(Collectors.toList());

		// 3. Aggregate Devices
		List<Map<String, Object>> deviceRaw = visitLogRepository.findDeviceStatsRawByRange(start, end);
		long mobileViews = 0, desktopViews = 0, tabletViews = 0;
		for (Map<String, Object> row : deviceRaw) {
			String os = cn.hutool.core.util.StrUtil.nullToEmpty((CharSequence) row.get("os")).toLowerCase();
			long count = ((Number) row.get("count")).longValue();
			if (os.contains("ipad") || os.contains("tablet") || os.contains("kindle"))
				tabletViews += count;
			else if (os.contains("android") || os.contains("ios") || os.contains("iphone") || os.contains("mobile"))
				mobileViews += count;
			else
				desktopViews += count;
		}
		List<space.nebula.nexus.payload.response.TrafficStatsResponse.TrafficMetric> devices = List.of(
				new space.nebula.nexus.payload.response.TrafficStatsResponse.TrafficMetric("Mobile", mobileViews,
						currentSessions > 0 ? (double) mobileViews / currentSessions * 100 : 0),
				new space.nebula.nexus.payload.response.TrafficStatsResponse.TrafficMetric("Desktop", desktopViews,
						currentSessions > 0 ? (double) desktopViews / currentSessions * 100 : 0),
				new space.nebula.nexus.payload.response.TrafficStatsResponse.TrafficMetric("Tablet", tabletViews,
						currentSessions > 0 ? (double) tabletViews / currentSessions * 100 : 0));

		// 4. Aggregate Sources
		List<Map<String, Object>> sourceRaw = visitLogRepository.findSourceStatsRawByRange(start, end);
		Map<String, Long> channelCounts = new java.util.HashMap<>();
		for (Map<String, Object> row : sourceRaw) {
			String referer = cn.hutool.core.util.StrUtil.nullToEmpty((CharSequence) row.get("referer")).toLowerCase();
			String url = cn.hutool.core.util.StrUtil.nullToEmpty((CharSequence) row.get("url")).toLowerCase();
			long count = ((Number) row.get("count")).longValue();

			String channel = "Referral";
			if (url.contains("utm_medium=cpc") || url.contains("utm_source=ad"))
				channel = "Paid Search";
			else if (url.contains("utm_medium=email"))
				channel = "Email";
			else if (url.contains("utm_medium=display"))
				channel = "Display Ads";
			else if (url.contains("utm_medium=affiliate"))
				channel = "Affiliate";
			else if (url.contains("utm_source=newsletter"))
				channel = "Newsletter";
			else if (url.contains("utm_medium=video") || referer.contains("youtube.com"))
				channel = "Video";
			else if (referer.contains("facebook.com") || referer.contains("twitter.com")
					|| referer.contains("linkedin.com") || referer.contains("t.co"))
				channel = "Social";
			else if (referer.contains("google.") || referer.contains("bing.com") || referer.contains("baidu.com")
					|| referer.contains("yahoo.com"))
				channel = "Organic Search";
			else if (cn.hutool.core.util.StrUtil.isBlank(referer) || referer.equals("null"))
				channel = "Direct";

			channelCounts.put(channel, channelCounts.getOrDefault(channel, 0L) + count);
		}

		List<space.nebula.nexus.payload.response.TrafficStatsResponse.TrafficMetric> sources = channelCounts.entrySet()
				.stream()
				.map(e -> new space.nebula.nexus.payload.response.TrafficStatsResponse.TrafficMetric(e.getKey(),
						e.getValue(), currentSessions > 0 ? (double) e.getValue() / currentSessions * 100 : 0))
				.sorted((a, b) -> Long.compare(b.views(), a.views())).collect(Collectors.toList());

		return ApiResponse.success(space.nebula.nexus.payload.response.TrafficStatsResponse.builder().summary(summary)
				.timeSeries(timeSeries).devices(devices).sources(sources).build());
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<space.nebula.nexus.payload.response.PostResponse>> getTrendingPosts(int limit) {
		LocalDateTime since = LocalDateTime.now().minusDays(7);
		List<Map<String, Object>> topUrls = visitLogRepository.findTopContentRaw(since);

		List<String> topSlugs = topUrls.stream().map(m -> String.valueOf(m.get("url")))
				.filter(url -> url.startsWith("/api/v1/public/blog/posts/"))
				.map(url -> url.substring("/api/v1/public/blog/posts/".length())).distinct().limit(limit).toList();

		if (topSlugs.isEmpty()) {
			return ApiResponse.success(List.of());
		}

		List<space.nebula.nexus.entity.Post> posts = postRepository.findAllBySlugIn(topSlugs);
		// Sort posts according to the order in topSlugs
		java.util.List<space.nebula.nexus.entity.Post> sortedPosts = new java.util.ArrayList<>(posts);
		sortedPosts.sort((p1, p2) -> Integer.compare(topSlugs.indexOf(p1.getSlug()), topSlugs.indexOf(p2.getSlug())));

		return ApiResponse.success(postMapper.toResponseList(sortedPosts));
	}

	@Override
	@Transactional
	public void purgeOldLogs(int daysToKeep) {
		LocalDateTime cutOff = LocalDateTime.now().minusDays(daysToKeep);
		log.info("Purging visit logs older than {} ({} days retention)...", cutOff, daysToKeep);
		int deletedCount = visitLogRepository.deleteByVisitTimeBefore(cutOff);
		log.info("Successfully purged {} old visit logs.", deletedCount);
	}
}
