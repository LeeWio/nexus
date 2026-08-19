package space.nebula.nexus.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.repository.ContentAnalyticsEventRepository;
import space.nebula.nexus.repository.PostRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class PublicAnalyticsControllerIntegrationTest {

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PostRepository postRepository;
	@Autowired
	private ContentAnalyticsEventRepository contentAnalyticsEventRepository;

	private Long postId;

	@BeforeEach
	void setUp() {
		User author = userRepository.findByUsername("analytics-author").orElseGet(() -> {
			User created = new User();
			created.setUsername("analytics-author");
			created.setPassword("password");
			created.setEmail("analytics-author@example.com");
			created.setStatus(UserStatus.ACTIVE);
			return userRepository.save(created);
		});
		Post post = new Post();
		post.setTitle("Analytics test post");
		post.setSlug("analytics-test-post");
		post.setContent("analytics content");
		post.setAuthor(author);
		post.setStatus(PostStatus.PUBLISHED);
		postId = postRepository.save(post).getId();
	}

	@Test
	void recordsReadingMilestonesWithOnlyAnonymousCookieState() throws Exception {
		long before = contentAnalyticsEventRepository.count();

		mockMvc.perform(post("/api/v1/public/analytics/content-events").contentType(MediaType.APPLICATION_JSON)
				.content("{\"action\":\"READING_PROGRESS\",\"postId\":" + postId
						+ ",\"progressPercent\":90,\"activeSeconds\":140}"))
				.andExpect(status().isOk());

		assertEquals(before + 4, contentAnalyticsEventRepository.count());
	}
}
