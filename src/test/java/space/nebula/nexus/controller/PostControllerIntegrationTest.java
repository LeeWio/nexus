package space.nebula.nexus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.Category;
import space.nebula.nexus.entity.User;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.enums.UserStatus;
import space.nebula.nexus.payload.request.PostRequest;
import space.nebula.nexus.repository.CategoryRepository;
import space.nebula.nexus.repository.UserRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.IInteractionService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.HashSet;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {space.nebula.nexus.config.MockRedisConfig.class,
		space.nebula.nexus.config.MockRabbitMQConfig.class})
@org.springframework.test.context.ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
public class PostControllerIntegrationTest {

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@MockitoBean
	private IInteractionService interactionService;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private UserRepository userRepository;

	private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

	private Long categoryId;

	@BeforeEach
	public void setup() {
		// Create a test user
		if (userRepository.findByUsername("admin").isEmpty()) {
			User admin = new User();
			admin.setUsername("admin");
			admin.setPassword("password");
			admin.setEmail("admin@example.com");
			admin.setStatus(UserStatus.ACTIVE);
			userRepository.save(admin);
		}

		// Create a test category
		Category category = new Category();
		category.setName("Test Category");
		category.setSlug("test-category");
		category = categoryRepository.save(category);
		categoryId = category.getId();
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testCreateAndGetPost() throws Exception {
		PostRequest request = new PostRequest("Test Post Title", "test-post-slug", null, "Summary of test post",
				"Content of test post", null, PostStatus.PUBLISHED, false, categoryId, null, null, null,
				new HashSet<>());

		// 1. Bearer-authenticated API writes must not require a cookie CSRF token.
		mockMvc.perform(post("/api/v1/admin/posts").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("Test Post Title"))
				.andExpect(jsonPath("$.data.slug").value("test-post-slug"));

		// 2. Get Public Posts
		mockMvc.perform(get("/api/v1/public/blog/posts").param("page", "1").param("size", "10"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.list").isArray());

		// 3. Get curated prominent posts without forcing the frontend to page through
		// all posts.
		mockMvc.perform(get("/api/v1/public/blog/posts/featured").param("page", "1").param("size", "10"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.list").isArray());

		// 4. Get archive and facet metadata for browse surfaces.
		mockMvc.perform(get("/api/v1/public/blog/archive").param("page", "1").param("size", "10"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.list").isArray());
		mockMvc.perform(get("/api/v1/public/blog/facets")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalPublishedCount").isNumber())
				.andExpect(jsonPath("$.data.categories").isArray()).andExpect(jsonPath("$.data.tags").isArray())
				.andExpect(jsonPath("$.data.archives").isArray()).andExpect(jsonPath("$.data.contentTypes").isArray());
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testUpdatePost() throws Exception {
		PostRequest createRequest = new PostRequest("Original Title", "original-slug", null, "Summary", "Content", null,
				PostStatus.DRAFT, false, categoryId, null, null, null, null);

		String response = mockMvc
				.perform(post("/api/v1/admin/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createRequest)))
				.andReturn().getResponse().getContentAsString();

		Long postId = objectMapper.readTree(response).get("data").get("id").asLong();

		PostRequest updateRequest = new PostRequest("Updated Title", "updated-slug", null, "Updated Summary",
				"Updated Content", null, PostStatus.PUBLISHED, true, categoryId, null, null, null, null);

		mockMvc.perform(put("/api/v1/admin/posts/" + postId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateRequest))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("Updated Title"))
				.andExpect(jsonPath("$.data.isFeatured").value(true));
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testRevisionTimelineDetailRestoreAndStaleWriteProtection() throws Exception {
		PostRequest initialRequest = new PostRequest("Revision source", "revision-source", null, "Original summary",
				"Original content", null, PostStatus.DRAFT, false, categoryId, null, null, null, null);
		String created = mockMvc
				.perform(post("/api/v1/admin/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(initialRequest)))
				.andReturn().getResponse().getContentAsString();
		Long postId = objectMapper.readTree(created).get("data").get("id").asLong();

		String firstTimeline = mockMvc.perform(get("/api/v1/admin/posts/" + postId + "/revisions/summary"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data[0].versionNumber").value(1)).andReturn()
				.getResponse().getContentAsString();
		Long firstRevisionId = objectMapper.readTree(firstTimeline).get("data").get(0).get("id").asLong();

		PostRequest updatedRequest = new PostRequest("Revision source updated", "revision-source", null,
				"Updated summary", "Updated content", null, PostStatus.DRAFT, false, categoryId, null, null, null,
				null);
		mockMvc.perform(put("/api/v1/admin/posts/" + postId).with(csrf()).header("If-Match", "\"revision-1\"")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updatedRequest)))
				.andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/admin/posts/" + postId).with(csrf()).header("If-Match", "revision-1")
				.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(updatedRequest)))
				.andExpect(status().isConflict());

		mockMvc.perform(get("/api/v1/admin/posts/" + postId + "/revisions/" + firstRevisionId))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.snapshot.content").value("Original content"));

		mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/revisions/" + firstRevisionId + "/revert").with(csrf())
				.header("If-Match", "revision-2")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content").value("Original content"));

		mockMvc.perform(get("/api/v1/admin/posts/" + postId + "/revisions/summary")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.length()").value(3))
				.andExpect(jsonPath("$.data[0].revisionKind").value("RESTORED"));
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testCopyAndBatchDeletePosts() throws Exception {
		PostRequest createRequest = new PostRequest("Copy Source", "copy-source", null, "Summary", "Content", null,
				PostStatus.DRAFT, false, categoryId, null, null, null, null);
		String created = mockMvc
				.perform(post("/api/v1/admin/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createRequest)))
				.andReturn().getResponse().getContentAsString();
		Long sourceId = objectMapper.readTree(created).get("data").get("id").asLong();

		String copied = mockMvc.perform(post("/api/v1/admin/posts/" + sourceId + "/copy").with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Copy Source (Copy)"))
				.andExpect(jsonPath("$.data.status").value("DRAFT")).andReturn().getResponse().getContentAsString();
		Long copiedId = objectMapper.readTree(copied).get("data").get("id").asLong();

		mockMvc.perform(delete("/api/v1/admin/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"ids\":[" + sourceId + "," + copiedId + "]}")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin", roles = {"ADMIN"})
	public void testCompleteEditorialLifecycle() throws Exception {
		PostRequest createRequest = new PostRequest("Lifecycle Post", "lifecycle-post", null, "Summary", "Content",
				null, PostStatus.DRAFT, false, categoryId, null, null, null, null);
		String response = mockMvc
				.perform(post("/api/v1/admin/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(createRequest)))
				.andReturn().getResponse().getContentAsString();
		Long postId = objectMapper.readTree(response).get("data").get("id").asLong();

		mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/submit").with(csrf())).andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/withdraw").with(csrf())).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/admin/posts/" + postId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DRAFT"));

		mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/submit").with(csrf())).andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/review").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"approved\":true,\"reviewComment\":null}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/archive").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Requires substantial revision\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/admin/posts/" + postId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("ARCHIVED"))
				.andExpect(jsonPath("$.data.archiveReason").value("Requires substantial revision"))
				.andExpect(jsonPath("$.data.archivedByName").value("admin"));

		mockMvc.perform(get("/api/v1/public/blog/posts/lifecycle-post")).andExpect(status().isForbidden());

		mockMvc.perform(post("/api/v1/admin/posts/" + postId + "/restore").with(csrf())).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/admin/posts/" + postId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("DRAFT"))
				.andExpect(jsonPath("$.data.archiveReason").doesNotExist())
				.andExpect(jsonPath("$.data.publishedAt").doesNotExist());
	}
}
