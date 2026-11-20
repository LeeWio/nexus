package space.nebula.nexus.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.CategoryFollow;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostContentType;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.repository.CategoryFollowRepository;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.IInteractionService;

import java.time.Instant;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@EnabledIfSystemProperty(named = "spring.datasource.driver-class-name", matches = "com.mysql.cj.jdbc.Driver")
class NotificationReadingMySqlIntegrationTest {

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@MockitoBean
	private IInteractionService interactionService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private CategoryFollowRepository categoryFollowRepository;

	private final ObjectMapper testObjectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private String readerUsername;
	private String publisherUsername;
	private Category category;

	@BeforeEach
	void createJourneyFixture() {
		String suffix = Long.toString(System.nanoTime());
		readerUsername = "mysql-reader-" + suffix;
		publisherUsername = "mysql-publisher-" + suffix;
		User reader = buildUser(readerUsername, "reader-" + suffix + "@example.test");
		User publisher = buildUser(publisherUsername, "publisher-" + suffix + "@example.test");
		userRepository.save(reader);
		userRepository.saveAndFlush(publisher);
		category = new Category();
		category.setName("MySQL Notification " + suffix);
		category.setSlug("mysql-notification-" + suffix);
		categoryRepository.saveAndFlush(category);
		CategoryFollow follow = new CategoryFollow();
		follow.setUser(reader);
		follow.setCategory(category);
		categoryFollowRepository.saveAndFlush(follow);
	}

	@Test
	void publicationCreatesCanonicalNotificationAndReadingStatePersists() throws Exception {
		String slug = "mysql-notification-journey-" + System.nanoTime();
		String title = "MySQL notification journey";
		String content = "{\"type\":\"doc\",\"content\":[{\"type\":\"heading\",\"attrs\":{\"level\":1},\"content\":[{\"type\":\"text\",\"text\":\"Journey\"}]},{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"A real backend reading journey.\"}]}]}";
		PostRequest request = new PostRequest(title, slug, null, null, "Journey summary", content, PostContentType.JSON,
				PostStatus.PUBLISHED, false, category.getId(), null, null, null, Set.of());

		String created = mockMvc
				.perform(post("/api/v1/admin/posts").with(user(publisherUsername).roles("ADMIN"))
						.contentType(MediaType.APPLICATION_JSON).content(testObjectMapper.writeValueAsString(request)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.slug").value(slug)).andReturn().getResponse()
				.getContentAsString();
		JsonNode createdData = testObjectMapper.readTree(created).path("data");
		long postId = createdData.path("id").asLong();

		mockMvc.perform(get("/api/v1/user/notifications").with(user(readerUsername).roles("USER"))
				.param("view", "inbox").param("size", "20")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.list[0].type").value("CATEGORY_POST"))
				.andExpect(jsonPath("$.data.list[0].link").value("/single/" + slug))
				.andExpect(jsonPath("$.data.list[0].context.objectType").value("POST"))
				.andExpect(jsonPath("$.data.list[0].context.action").value("VIEW"));

		mockMvc.perform(
				put("/api/v1/user/library/reading-list/{postId}", postId).with(user(readerUsername).roles("USER")))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/user/library/reading-list").with(user(readerUsername).roles("USER")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.list[0].post.slug").value(slug))
				.andExpect(jsonPath("$.data.list[0].addedAt").isNotEmpty());

		mockMvc.perform(put("/api/v1/user/library/posts/{postId}/progress", postId)
				.with(user(readerUsername).roles("USER")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"progressPercent\":65,\"positionAnchor\":\"journey\",\"observedAt\":\""
						+ Instant.now().minusSeconds(1) + "\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.currentProgressPercent").value(65))
				.andExpect(jsonPath("$.data.furthestProgressPercent").value(65))
				.andExpect(jsonPath("$.data.positionAnchor").value("journey"));

		mockMvc.perform(get("/api/v1/user/library/history").with(user(readerUsername).roles("USER")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.list[0].post.slug").value(slug))
				.andExpect(jsonPath("$.data.list[0].currentProgressPercent").value(65));
	}

	private User buildUser(String username, String email) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(email);
		user.setPassword("e2e-only-password");
		user.setStatus(UserStatus.ACTIVE);
		return user;
	}
}
