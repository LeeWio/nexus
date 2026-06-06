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
public class AnalyticsServiceImpl implements IAnalyticsService
{

	private final VisitLogRepository visitLogRepository;
	private final space.nebula.nexus.repository.DailyAnalyticsRepository dailyAnalyticsRepository;
	private final space.nebula.nexus.repository.CommentRepository commentRepository;
	private final space.nebula.nexus.repository.PostRepository postRepository;
	private final space.nebula.nexus.mapper.PostMapper postMapper;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = CacheConstants.ANALYTICS, key = CacheConstants.OVERVIEW_KEY)
	public ApiResponse<AnalyticsOverviewResponse> retrieveOverviewStats()
	{
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
				.stream()
				.map(m -> {
					String url = String.valueOf(m.get("url"));
					String title = url;
					if (url.startsWith("/api/v1/public/blog/posts/")) {
						String slug = url.substring("/api/v1/public/blog/posts/".length());
						title = postRepository.findBySlug(slug).map(space.nebula.nexus.entity.Post::getTitle).orElse(url);
					}
					return AnalyticsOverviewResponse.TopContentItem.builder()
						.url(url)
						.title(title)
						.count(((Number) m.get("count")).longValue())
						.build();
				})
				.limit(10).toList();

		// 2. Map Daily Trends (Mix of historical and real-time today)
		List<AnalyticsOverviewResponse.VisitTrendItem> dailyTrends = dailyAnalyticsRepository
				.findByStatDateGreaterThanEqualOrderByStatDateAsc(weekAgo).stream()
				.map(d -> AnalyticsOverviewResponse.VisitTrendItem.builder()
						.date(d.getStatDate().toString())
						.pv(d.getPv())
						.uv(d.getUv())
						.build())
				.collect(Collectors.toList());
		
		// Add today's real-time data to trend
		dailyTrends.add(AnalyticsOverviewResponse.VisitTrendItem.builder()
				.date(today.toString())
				.pv(todayPv)
				.uv(todayUv)
				.build());

		AnalyticsOverviewResponse response = AnalyticsOverviewResponse.builder()
				.todayPv(todayPv)
				.todayUv(todayUv)
				.yesterdayPv(yesterdayPv)
				.yesterdayUv(yesterdayUv)
				.pvGrowthRate(growthRate)
				.dailyTrends(dailyTrends)
				.topContent(topContent)
				.build();

		return ApiResponse.success(response);
	}

	@Override
	@Transactional
	public void aggregateDailyData(LocalDate date)
	{
		log.info("Aggregating analytics data for date: {}", date);
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.atTime(LocalTime.MAX);

		long pv = visitLogRepository.countPv(start, end);
		long uv = visitLogRepository.countUv(start, end);
		long comments = commentRepository.countByCreatedAtBetween(start, end);
		
		// Count post detail API hits as post views
		long postViews = visitLogRepository.findDailyTrendRaw(start).stream()
				.filter(m -> String.valueOf(m.get("visitDate")).equals(date.toString()))
				.mapToLong(m -> ((Number) m.get("pv")).longValue())
				.sum();

		var daily = dailyAnalyticsRepository.findByStatDate(date).orElse(new space.nebula.nexus.entity.DailyAnalytics());
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
	public ApiResponse<List<space.nebula.nexus.payload.response.PostResponse>> getTrendingPosts(int limit)
	{
		LocalDateTime since = LocalDateTime.now().minusDays(7);
		List<Map<String, Object>> topUrls = visitLogRepository.findTopContentRaw(since);

		List<String> topSlugs = topUrls.stream()
				.map(m -> String.valueOf(m.get("url")))
				.filter(url -> url.startsWith("/api/v1/public/blog/posts/"))
				.map(url -> url.substring("/api/v1/public/blog/posts/".length()))
				.distinct()
				.limit(limit)
				.toList();

		if (topSlugs.isEmpty()) {
			return ApiResponse.success(List.of());
		}

		List<space.nebula.nexus.entity.Post> posts = postRepository.findAllBySlugIn(topSlugs);
		// Sort posts according to the order in topSlugs
		java.util.List<space.nebula.nexus.entity.Post> sortedPosts = new java.util.ArrayList<>(posts);
		sortedPosts.sort((p1, p2) -> Integer.compare(topSlugs.indexOf(p1.getSlug()), topSlugs.indexOf(p2.getSlug())));

		return ApiResponse.success(postMapper.toResponseList(sortedPosts));
	}
}
