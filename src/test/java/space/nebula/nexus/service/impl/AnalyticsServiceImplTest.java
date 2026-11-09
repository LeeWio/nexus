package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.entity.DailyAnalytics;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.mapper.PostMapper;
import space.nebula.nexus.repository.CommentRepository;
import space.nebula.nexus.repository.DailyAnalyticsRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.PageViewRepository;
import space.nebula.nexus.repository.VisitLogRepository;
import space.nebula.nexus.repository.ContentAnalyticsEventRepository;
import space.nebula.nexus.repository.PostFavoriteRepository;
import space.nebula.nexus.repository.PostLikeRepository;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.enums.ContentAnalyticsAction;
import space.nebula.nexus.payload.request.ContentAnalyticsEventRequest;
import space.nebula.nexus.utils.ClientDeviceClassifier;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

	@Mock
	private VisitLogRepository visitLogRepository;
	@Mock
	private PageViewRepository pageViewRepository;
	@Mock
	private DailyAnalyticsRepository dailyAnalyticsRepository;
	@Mock
	private CommentRepository commentRepository;
	@Mock
	private PostRepository postRepository;
	@Mock
	private PostMapper postMapper;
	@Mock
	private ContentAnalyticsEventRepository contentAnalyticsEventRepository;
	@Mock
	private PostLikeRepository postLikeRepository;
	@Mock
	private PostFavoriteRepository postFavoriteRepository;
	@Mock
	private SubscriberRepository subscriberRepository;

	@InjectMocks
	private AnalyticsServiceImpl analyticsService;

	@Test
	void aggregateDailyData_Success() {
		LocalDate date = LocalDate.now().minusDays(1);

		when(pageViewRepository.countViews(any(), any(), any())).thenReturn(100L);
		when(pageViewRepository.countVisitors(any(), any(), any())).thenReturn(50L);
		when(pageViewRepository.countSessions(any(), any(), any())).thenReturn(40L);
		when(pageViewRepository.findTopPaths(any(), any(), any())).thenReturn(List.of());
		when(commentRepository.countByCreatedAtBetween(any(), any())).thenReturn(5L);
		when(dailyAnalyticsRepository.findByStatDate(date)).thenReturn(Optional.empty());

		analyticsService.aggregateDailyData(date);

		verify(dailyAnalyticsRepository).save(any(DailyAnalytics.class));
	}

	@Test
	void getTrendingPosts_Success() {
		String slug = "test-post";
		Map<String, Object> mockData = Map.of("path", "/single/" + slug, "views", 10L);

		when(pageViewRepository.findTopPaths(any(), any(), any())).thenReturn(List.of(mockData));
		when(postRepository.findAllBySlugIn(anyList())).thenReturn(List.of(new Post()));
		when(postMapper.toResponseList(any())).thenReturn(List.of());

		var response = analyticsService.getTrendingPosts(5);

		assertNotNull(response);
		assertEquals(200, response.code());
	}

	@Test
	void recordContentEvent_RecordsEachReadingMilestoneOnce() {
		Post post = new Post();
		post.setId(42L);
		post.setStatus(space.nebula.nexus.enums.PostStatus.PUBLISHED);
		when(postRepository.findById(42L)).thenReturn(Optional.of(post));
		when(contentAnalyticsEventRepository.existsBySessionIdAndPostIdAndEventTypeAndIsDeletedFalse(any(), any(),
				any())).thenReturn(false);

		var response = analyticsService.recordContentEvent(
				new ContentAnalyticsEventRequest(ContentAnalyticsAction.READING_PROGRESS, 42L, 90, 180),
				"2f4093b5-f1fb-4b9f-86ce-1fc5f7e10c71", "visitor-hash");

		assertEquals(200, response.code());
		verify(contentAnalyticsEventRepository, times(4)).save(any());
	}

	@Test
	void getTrafficStats_UsesStoredDeviceBuckets() {
		when(pageViewRepository.countSessions(any(), any(), any())).thenReturn(4L);
		when(pageViewRepository.countVisitors(any(), any(), any())).thenReturn(3L);
		when(pageViewRepository.countReturningVisitors(any(), any(), any())).thenReturn(1L);
		when(pageViewRepository.countBouncedSessions(any(), any(), any())).thenReturn(1L);
		when(pageViewRepository.averageEngagedSeconds(any(), any(), any())).thenReturn(12.0);
		when(pageViewRepository.findDailyTrend(any(), any(), any())).thenReturn(List.of());
		when(pageViewRepository.countByChannel(any(), any(), any())).thenReturn(List.of());
		when(pageViewRepository.countByDevice(any(), any(), any()))
				.thenReturn(List.of(Map.of("name", ClientDeviceClassifier.MOBILE, "count", 2L),
						Map.of("name", ClientDeviceClassifier.DESKTOP, "count", 1L),
						Map.of("name", ClientDeviceClassifier.TABLET, "count", 1L)));

		var devices = analyticsService.getTrafficStats(30).data().devices();

		assertEquals(
				List.of(ClientDeviceClassifier.MOBILE, ClientDeviceClassifier.DESKTOP, ClientDeviceClassifier.TABLET),
				devices.stream().map(device -> device.name()).toList());
		assertEquals(List.of(2L, 1L, 1L), devices.stream().map(device -> device.views()).toList());
		assertEquals(50.0, devices.get(0).percentage());
	}
}
