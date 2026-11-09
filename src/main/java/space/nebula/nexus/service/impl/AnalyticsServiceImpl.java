package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.ContentAnalyticsEvent;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.ContentAnalyticsAction;
import space.nebula.nexus.enums.ContentAnalyticsEventType;
import space.nebula.nexus.payload.request.ContentAnalyticsEventRequest;
import space.nebula.nexus.payload.response.AnalyticsOverviewResponse;
import space.nebula.nexus.payload.response.ContentFunnelResponse;
import space.nebula.nexus.payload.response.TopPageResponse;
import space.nebula.nexus.payload.response.TrafficStatsResponse;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.ContentAnalyticsEventRepository;
import space.nebula.nexus.repository.DailyAnalyticsRepository;
import space.nebula.nexus.repository.PostFavoriteRepository;
import space.nebula.nexus.repository.PostLikeRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.repository.PageViewRepository;
import space.nebula.nexus.repository.VisitLogRepository;
import space.nebula.nexus.service.IAnalyticsService;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.enums.TrafficClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * First-party analytics backed by anonymous sessions and durable event
 * milestones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements IAnalyticsService {
	private static final int MAX_LOOKBACK_DAYS = 365;
	private static final List<TrafficClass> READER = List.of(TrafficClass.HUMAN);
	private static final List<String> READER_NAMES = List.of(TrafficClass.HUMAN.name());

	private final VisitLogRepository visitLogRepository;
	private final PageViewRepository pageViewRepository;
	private final DailyAnalyticsRepository dailyAnalyticsRepository;
	private final CommentRepository commentRepository;
	private final PostRepository postRepository;
	private final PostMapper postMapper;
	private final ContentAnalyticsEventRepository contentAnalyticsEventRepository;
	private final PostLikeRepository postLikeRepository;
	private final PostFavoriteRepository postFavoriteRepository;
	private final SubscriberRepository subscriberRepository;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<AnalyticsOverviewResponse> retrieveOverviewStats() {
		LocalDate today = LocalDate.now();
		LocalDate yesterday = today.minusDays(1);
		LocalDate weekAgo = today.minusDays(7);
		LocalDateTime todayStart = today.atStartOfDay();
		LocalDateTime todayEnd = today.atTime(LocalTime.MAX);
		long todayPv = pageViewRepository.countViews(todayStart, todayEnd, READER);
		long todayUv = pageViewRepository.countVisitors(todayStart, todayEnd, READER);

		var yesterdayData = dailyAnalyticsRepository.findByStatDate(yesterday);
		long yesterdayPv = yesterdayData.map(space.nebula.nexus.entity.DailyAnalytics::getPv)
				.orElseGet(() -> pageViewRepository.countViews(yesterday.atStartOfDay(), todayStart, READER));
		long yesterdayUv = yesterdayData.map(space.nebula.nexus.entity.DailyAnalytics::getUv)
				.orElseGet(() -> pageViewRepository.countVisitors(yesterday.atStartOfDay(), todayStart, READER));
		double growthRate = growth(todayPv, yesterdayPv);

		List<AnalyticsOverviewResponse.TopContentItem> topContent = pageViewRepository
				.findTopPaths(todayStart, todayEnd, READER_NAMES).stream().map(this::toTopContentItem).limit(10)
				.toList();

		List<AnalyticsOverviewResponse.VisitTrendItem> dailyTrends = dailyAnalyticsRepository
				.findByStatDateGreaterThanEqualOrderByStatDateAsc(weekAgo).stream()
				.map(daily -> AnalyticsOverviewResponse.VisitTrendItem.builder().date(daily.getStatDate().toString())
						.pv(daily.getPv()).uv(daily.getUv()).build())
				.collect(Collectors.toList());
		dailyTrends.add(AnalyticsOverviewResponse.VisitTrendItem.builder().date(today.toString()).pv(todayPv)
				.uv(todayUv).build());

		return ApiResponse.success(AnalyticsOverviewResponse.builder().todayPv(todayPv).todayUv(todayUv)
				.yesterdayPv(yesterdayPv).yesterdayUv(yesterdayUv).pvGrowthRate(growthRate).dailyTrends(dailyTrends)
				.topContent(topContent).build());
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<TopPageResponse>> getTopPages() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
		LocalDateTime todayEnd = now.toLocalDate().atTime(LocalTime.MAX);
		LocalDateTime yesterdayStart = todayStart.minusDays(1);

		List<Map<String, Object>> todayContent = pageViewRepository.findTopPaths(todayStart, todayEnd, READER_NAMES);
		Map<String, Long> yesterdayCounts = pageViewRepository.findTopPaths(yesterdayStart, todayStart, READER_NAMES)
				.stream().collect(Collectors.toMap(row -> String.valueOf(row.get("path")),
						row -> rowCount(row, "views"), (left, right) -> left));

		List<TopPageResponse> topPages = todayContent.stream().map(row -> {
			String url = String.valueOf(row.get("path"));
			long views = rowCount(row, "views");
			long previousViews = yesterdayCounts.getOrDefault(url, 0L);
			return TopPageResponse.builder().path(url).views(views).avgTime("-").bounceRate(0)
					.trend(formatGrowth(views, previousViews)).build();
		}).limit(20).toList();

		return ApiResponse.success(topPages);
	}

	@Override
	@Transactional
	public ApiResponse<Void> recordContentEvent(ContentAnalyticsEventRequest request, String sessionId,
			String visitorHash) {
		Post post = postRepository.findById(request.postId())
				.orElseThrow(() -> new space.nebula.nexus.common.exception.ResourceNotFoundException("Post", "id",
						request.postId()));
		Assert.isTrue(post.isPublished(),
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Only published posts can be tracked"));

		if (request.action() == ContentAnalyticsAction.IMPRESSION) {
			recordOnce(sessionId, visitorHash, post.getId(), ContentAnalyticsEventType.POST_IMPRESSION, null, null);
		} else if (request.action() == ContentAnalyticsAction.CLICK) {
			recordOnce(sessionId, visitorHash, post.getId(), ContentAnalyticsEventType.POST_CLICK, null, null);
		} else {
			recordReadingMilestones(request, sessionId, visitorHash, post.getId());
		}
		return ApiResponse.success(null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<ContentFunnelResponse> getContentFunnel(int days, Long postId) {
		int lookbackDays = normalizeLookback(days);
		LocalDateTime end = LocalDateTime.now();
		LocalDateTime start = end.minusDays(lookbackDays).toLocalDate().atStartOfDay();

		long impressions = countMilestone(ContentAnalyticsEventType.POST_IMPRESSION, start, end, postId);
		long clicks = countMilestone(ContentAnalyticsEventType.POST_CLICK, start, end, postId);
		long readers25 = countMilestone(ContentAnalyticsEventType.READ_25, start, end, postId);
		long readers50 = countMilestone(ContentAnalyticsEventType.READ_50, start, end, postId);
		long readers75 = countMilestone(ContentAnalyticsEventType.READ_75, start, end, postId);
		long completed = countMilestone(ContentAnalyticsEventType.READ_COMPLETE, start, end, postId);
		long likes = postLikeRepository.countCreatedBetween(start, end, postId);
		long favorites = postFavoriteRepository.countCreatedBetween(start, end, postId);
		long verifiedSubscriptions = postId == null
				? subscriberRepository.countByStatusAndVerifiedAtBetween(SubscriberStatus.ACTIVE, start, end)
				: 0L;
		long returningVisitors = postId == null ? visitLogRepository.countReturningVisitors(start, end) : 0L;
		double averageReadSeconds = numberOrZero(
				contentAnalyticsEventRepository.findAverageActiveSecondsByEventTypeAndPeriod(
						ContentAnalyticsEventType.READ_COMPLETE, start, end, postId));

		return ApiResponse.success(ContentFunnelResponse.builder().postId(postId).start(start).end(end)
				.impressions(impressions).clicks(clicks).readers25Percent(readers25).readers50Percent(readers50)
				.readers75Percent(readers75).completedReads(completed).likes(likes).favorites(favorites)
				.verifiedSubscriptions(verifiedSubscriptions).returningVisitors(returningVisitors)
				.averageActiveReadSeconds(averageReadSeconds).clickThroughRate(percentage(clicks, impressions))
				.completionRate(percentage(completed, clicks)).build());
	}

	@Override
	@Transactional
	public void aggregateDailyData(LocalDate date) {
		LocalDateTime start = date.atStartOfDay();
		LocalDateTime end = date.atTime(LocalTime.MAX);
		long pv = pageViewRepository.countViews(start, end, READER);
		long uv = pageViewRepository.countVisitors(start, end, READER);
		long sessions = pageViewRepository.countSessions(start, end, READER);
		long comments = commentRepository.countByCreatedAtBetween(start, end);
		long postViews = pageViewRepository.findTopPaths(start, end, READER_NAMES).stream()
				.filter(row -> String.valueOf(row.get("path")).startsWith("/single/"))
				.mapToLong(row -> rowCount(row, "views")).sum();
		long ownerPv = pageViewRepository.countViews(start, end, List.of(TrafficClass.OWNER));
		long botPv = pageViewRepository.countViews(start, end, List.of(TrafficClass.BOT));

		var daily = dailyAnalyticsRepository.findByStatDate(date)
				.orElse(new space.nebula.nexus.entity.DailyAnalytics());
		daily.setStatDate(date);
		daily.setPv(pv);
		daily.setUv(uv);
		daily.setSessions(sessions);
		daily.setCommentCount(comments);
		daily.setPostViews(postViews);
		daily.setOwnerPv(ownerPv);
		daily.setBotPv(botPv);
		dailyAnalyticsRepository.save(daily);
		log.info("Daily aggregation completed for {}: PV={}, UV={}", date, pv, uv);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<TrafficStatsResponse> getTrafficStats(int days) {
		int lookbackDays = normalizeLookback(days);
		LocalDateTime end = LocalDateTime.now();
		LocalDateTime start = end.minusDays(lookbackDays).toLocalDate().atStartOfDay();
		LocalDateTime previousStart = start.minusDays(lookbackDays);

		long currentSessions = pageViewRepository.countSessions(start, end, READER);
		long currentUsers = pageViewRepository.countVisitors(start, end, READER);
		long currentReturningVisitors = pageViewRepository.countReturningVisitors(start, end, READER_NAMES);
		long currentBouncedSessions = pageViewRepository.countBouncedSessions(start, end, READER_NAMES);
		double currentBounce = percentage(currentBouncedSessions, currentSessions);
		double currentAverageSeconds = numberOrZero(pageViewRepository.averageEngagedSeconds(start, end, READER_NAMES));

		long previousSessions = pageViewRepository.countSessions(previousStart, start, READER);
		long previousUsers = pageViewRepository.countVisitors(previousStart, start, READER);
		long previousReturningVisitors = pageViewRepository.countReturningVisitors(previousStart, start, READER_NAMES);
		long previousBouncedSessions = pageViewRepository.countBouncedSessions(previousStart, start, READER_NAMES);
		double previousBounce = percentage(previousBouncedSessions, previousSessions);
		double previousAverageSeconds = numberOrZero(
				pageViewRepository.averageEngagedSeconds(previousStart, start, READER_NAMES));

		TrafficStatsResponse.SummaryMetrics summary = TrafficStatsResponse.SummaryMetrics.builder()
				.sessions(metric(currentSessions, growth(currentSessions, previousSessions),
						String.format("%,d", currentSessions)))
				.users(metric(currentUsers, growth(currentUsers, previousUsers), String.format("%,d", currentUsers)))
				.returningVisitors(
						metric(currentReturningVisitors, growth(currentReturningVisitors, previousReturningVisitors),
								String.format("%,d", currentReturningVisitors)))
				.bounceRate(metric(currentBounce, growth(currentBounce, previousBounce), formatPercent(currentBounce)))
				.avgSession(metric(currentAverageSeconds, growth(currentAverageSeconds, previousAverageSeconds),
						formatDuration(currentAverageSeconds)))
				.build();

		List<TrafficStatsResponse.TimeSeriesItem> timeSeries = pageViewRepository
				.findDailyTrend(start, end, READER_NAMES).stream()
				.map(row -> TrafficStatsResponse.TimeSeriesItem.builder().date(String.valueOf(row.get("visitDate")))
						.sessions(rowCount(row, "sessions")).users(rowCount(row, "users")).build())
				.toList();

		List<TrafficStatsResponse.TrafficMetric> devices = namedMetrics(
				pageViewRepository.countByDevice(start, end, READER_NAMES), currentSessions);
		List<TrafficStatsResponse.TrafficMetric> sources = namedMetrics(
				pageViewRepository.countByChannel(start, end, READER_NAMES), currentSessions);
		return ApiResponse.success(TrafficStatsResponse.builder().summary(summary).timeSeries(timeSeries)
				.devices(devices).sources(sources).build());
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<space.nebula.nexus.payload.response.PostResponse>> getTrendingPosts(int limit) {
		int safeLimit = Math.clamp(limit, 1, 50);
		LocalDateTime since = LocalDateTime.now().minusDays(7);
		List<String> topSlugs = pageViewRepository.findTopPaths(since, LocalDateTime.now(), READER_NAMES).stream()
				.map(row -> String.valueOf(row.get("path"))).filter(path -> path.startsWith("/single/"))
				.map(path -> path.substring("/single/".length())).filter(slug -> !slug.isBlank() && !slug.contains("/"))
				.distinct().limit(safeLimit).toList();
		if (topSlugs.isEmpty()) {
			return ApiResponse.success(List.of());
		}
		List<Post> posts = new java.util.ArrayList<>(postRepository.findAllBySlugIn(topSlugs));
		posts.sort(
				(left, right) -> Integer.compare(topSlugs.indexOf(left.getSlug()), topSlugs.indexOf(right.getSlug())));
		return ApiResponse.success(postMapper.toResponseList(posts));
	}

	@Override
	@Transactional
	public void purgeOldLogs(int daysToKeep) {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(Math.max(1, daysToKeep));
		int requestLogsDeleted = visitLogRepository.deleteByVisitTimeBefore(cutoff);
		int pageViewsDeleted = pageViewRepository.deleteByViewedAtBefore(cutoff);
		long eventsDeleted = contentAnalyticsEventRepository.deleteByCreatedAtBefore(cutoff);
		log.info("Purged {} request logs, {} page views, and {} content events before {}", requestLogsDeleted,
				pageViewsDeleted, eventsDeleted, cutoff);
	}

	private void recordReadingMilestones(ContentAnalyticsEventRequest request, String sessionId, String visitorHash,
			Long postId) {
		Assert.notNull(request.progressPercent(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"progressPercent is required for a reading progress event"));
		Assert.notNull(request.activeSeconds(), () -> new BusinessException(BusinessCode.BAD_REQUEST,
				"activeSeconds is required for a reading progress event"));
		int progress = request.progressPercent();
		if (progress >= 25) {
			recordOnce(sessionId, visitorHash, postId, ContentAnalyticsEventType.READ_25, progress,
					request.activeSeconds());
		}
		if (progress >= 50) {
			recordOnce(sessionId, visitorHash, postId, ContentAnalyticsEventType.READ_50, progress,
					request.activeSeconds());
		}
		if (progress >= 75) {
			recordOnce(sessionId, visitorHash, postId, ContentAnalyticsEventType.READ_75, progress,
					request.activeSeconds());
		}
		if (progress >= 90) {
			recordOnce(sessionId, visitorHash, postId, ContentAnalyticsEventType.READ_COMPLETE, progress,
					request.activeSeconds());
		}
	}

	private void recordOnce(String sessionId, String visitorHash, Long postId, ContentAnalyticsEventType eventType,
			Integer progressPercent, Integer activeSeconds) {
		if (contentAnalyticsEventRepository.existsBySessionIdAndPostIdAndEventTypeAndIsDeletedFalse(sessionId, postId,
				eventType)) {
			return;
		}
		ContentAnalyticsEvent event = new ContentAnalyticsEvent();
		event.setSessionId(sessionId);
		event.setVisitorHash(visitorHash);
		event.setPostId(postId);
		event.setEventType(eventType);
		event.setProgressPercent(progressPercent);
		event.setActiveSeconds(activeSeconds);
		contentAnalyticsEventRepository.save(event);
	}

	private long countMilestone(ContentAnalyticsEventType eventType, LocalDateTime start, LocalDateTime end,
			Long postId) {
		return contentAnalyticsEventRepository.countDistinctSessionsByEventTypeAndPeriod(eventType, start, end, postId);
	}

	private List<TrafficStatsResponse.TrafficMetric> namedMetrics(List<Map<String, Object>> rows, long total) {
		return rows.stream().map(row -> {
			long count = rowCount(row);
			String name = stringField(row, "name");
			return new TrafficStatsResponse.TrafficMetric(name.isBlank() ? "Unknown" : name, count,
					percentage(count, total));
		}).toList();
	}

	private AnalyticsOverviewResponse.TopContentItem toTopContentItem(Map<String, Object> row) {
		String url = String.valueOf(row.get("path"));
		String title = url;
		if (url.startsWith("/single/")) {
			String slug = url.substring("/single/".length());
			title = postRepository.findBySlug(slug).map(Post::getTitle).orElse(url);
		}
		return AnalyticsOverviewResponse.TopContentItem.builder().url(url).title(title).count(rowCount(row, "views"))
				.build();
	}

	private TrafficStatsResponse.Metric metric(double value, double growth, String display) {
		return new TrafficStatsResponse.Metric(display, value, growth);
	}

	private int normalizeLookback(int days) {
		return Math.clamp(days, 1, MAX_LOOKBACK_DAYS);
	}

	private long rowCount(Map<String, Object> row) {
		return rowCount(row, "count");
	}

	private long rowCount(Map<String, Object> row, String field) {
		Object value = row.get(field);
		return value instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(value));
	}

	private String stringField(Map<String, Object> row, String field) {
		Object value = row.get(field);
		return value == null ? "" : String.valueOf(value);
	}

	private double numberOrZero(Double value) {
		return value == null ? 0 : value;
	}

	private double percentage(long numerator, long denominator) {
		return denominator == 0 ? 0 : (numerator * 100.0) / denominator;
	}

	private double growth(double current, double previous) {
		return previous == 0 ? 0 : ((current - previous) / previous) * 100;
	}

	private String formatGrowth(long current, long previous) {
		if (previous == 0)
			return current > 0 ? "+100%" : "0%";
		double value = growth(current, previous);
		return (value > 0 ? "+" : "") + String.format("%.1f%%", value);
	}

	private String formatPercent(double value) {
		return String.format("%.1f%%", value);
	}

	private String formatDuration(double seconds) {
		long totalSeconds = Math.max(0, Math.round(seconds));
		return String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
	}
}
