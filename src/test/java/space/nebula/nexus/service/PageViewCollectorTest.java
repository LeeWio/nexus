package space.nebula.nexus.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.data.redis.serializer.RedisSerializer;
import space.nebula.nexus.common.constant.CacheConstants;
import space.nebula.nexus.config.AnalyticsProperties;
import space.nebula.nexus.entity.PageView;
import space.nebula.nexus.payload.request.PageViewRequest;
import space.nebula.nexus.utils.RedisUtil;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PageViewCollectorTest {

	@Test
	void instantRoundTripsThroughTheProductionRedisSerializer() {
		PageView view = new PageView();
		view.setViewedAt(Instant.parse("2026-11-16T06:37:00.123456Z"));
		RedisSerializer<Object> serializer = RedisSerializer.json();
		PageView restored = (PageView) serializer.deserialize(serializer.serialize(view));
		assertEquals(view.getViewedAt(), restored.getViewedAt());
	}

	@Test
	void buffersTheExactInstantEvenWhenTheClockUsesLosAngelesTime() {
		Instant instant = Instant.parse("2026-11-16T06:37:00Z");
		AnalyticsProperties properties = new AnalyticsProperties();
		properties.setHashSalt("analytics-test-salt");
		RedisUtil redis = mock(RedisUtil.class);
		when(redis.lock(anyString(), eq("1"), anyLong(), any())).thenReturn(true);
		when(redis.listAdd(eq(CacheConstants.PAGE_VIEW_BUFFER_KEY), any())).thenReturn(1L);
		var collector = new PageViewCollector(new AnalyticsPrivacyService(properties), properties,
				new PageViewNormalizer(), redis,
				new AnalyticsTime(Clock.fixed(instant, ZoneId.of("America/Los_Angeles"))));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr("203.0.113.8");
		request.addHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) Chrome/130.0.0.0 Safari/537.36");
		assertTrue(collector.collect(new PageViewRequest("/moments", null, "Moments"), "test-session", request));
		var view = ArgumentCaptor.forClass(PageView.class);
		verify(redis).listAdd(eq(CacheConstants.PAGE_VIEW_BUFFER_KEY), view.capture());
		assertEquals(instant, view.getValue().getViewedAt());
	}
}
