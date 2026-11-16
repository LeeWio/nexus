package space.nebula.nexus.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.entity.PageView;
import space.nebula.nexus.enums.TrafficClass;
import space.nebula.nexus.repository.PageViewRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.AnalyticsTime;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "analytics-admin", roles = "ADMIN")
class AdminAnalyticsTimeIntegrationTest {

	@MockitoBean
	private PostSearchRepository postSearchRepository;
	@MockitoBean
	private AnalyticsTime analyticsTime;
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private PageViewRepository repository;

	@BeforeEach
	void fixture() {
		repository.deleteAll();
		when(analyticsTime.recentDays(7)).thenReturn(
				new AnalyticsTime.Window(Instant.parse("2026-11-09T16:00:00Z"), Instant.parse("2026-11-16T07:00:00Z")));
		PageView view = new PageView();
		view.setViewedAt(Instant.parse("2026-11-16T06:37:00Z"));
		view.setPath("/moments");
		view.setSessionId("analytics-time-session");
		view.setVisitorHash("analytics-time-visitor");
		view.setSourceChannel("Direct");
		view.setTrafficClass(TrafficClass.HUMAN);
		view.setClientName("Chrome");
		view.setOsName("Linux");
		view.setDeviceType("Desktop");
		repository.saveAndFlush(view);
	}

	@Test
	void visitorSummarySerializesUtcInstantsThroughTheActualWebMapper() throws Exception {
		mockMvc.perform(get("/api/v1/admin/analytics/visitors").param("days", "7")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.list[0].firstSeen").value("2026-11-16T06:37:00Z"))
				.andExpect(jsonPath("$.data.list[0].lastSeen").value("2026-11-16T06:37:00Z"));
	}

	@Test
	void visitorTimelineSerializesTheSameUtcInstant() throws Exception {
		mockMvc.perform(get("/api/v1/admin/analytics/visitors/analytics-time-visitor").param("days", "7"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data[0].viewedAt").value("2026-11-16T06:37:00Z"));
	}
}
