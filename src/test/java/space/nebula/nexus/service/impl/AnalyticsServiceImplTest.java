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
import space.nebula.nexus.repository.VisitLogRepository;
import space.nebula.nexus.repository.ContentAnalyticsEventRepository;
import space.nebula.nexus.repository.PostFavoriteRepository;
import space.nebula.nexus.repository.PostLikeRepository;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.enums.ContentAnalyticsAction;
import space.nebula.nexus.payload.request.ContentAnalyticsEventRequest;

import java.time.LocalDate;
import java.util.Collections;
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

		when(visitLogRepository.countPv(any(), any())).thenReturn(100L);
		when(visitLogRepository.countUv(any(), any())).thenReturn(50L);
		when(commentRepository.countByCreatedAtBetween(any(), any())).thenReturn(5L);
		when(dailyAnalyticsRepository.findByStatDate(date)).thenReturn(Optional.empty());

		analyticsService.aggregateDailyData(date);

		verify(dailyAnalyticsRepository).save(any(DailyAnalytics.class));
	}

	@Test
	void getTrendingPosts_Success() {
		String slug = "test-post";
		Map<String, Object> mockData = Map.of("url", "/api/v1/public/blog/posts/" + slug, "count", 10L);

		when(visitLogRepository.findTopContentRaw(any())).thenReturn(List.of(mockData));
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
}
