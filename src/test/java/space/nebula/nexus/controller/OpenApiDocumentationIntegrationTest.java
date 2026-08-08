package space.nebula.nexus.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.IInteractionService;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Guards frontend-facing OpenAPI conventions that cannot be checked from
 * individual controller annotations alone.
 */
@SpringBootTest(classes = {space.nebula.nexus.config.MockRedisConfig.class,
		space.nebula.nexus.config.MockRabbitMQConfig.class}, properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationIntegrationTest {

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@MockitoBean
	private IInteractionService interactionService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void publishesConsistentFrontendContractAndAuthenticationMetadata() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.info.description").value(containsString("Frontend integration contract")))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.schemas.ApiError.properties.traceId").exists())
				.andExpect(jsonPath("$.paths['/api/v1/public/blog/posts'].get.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/user/notifications'].get.security[0].bearerAuth").isArray())
				.andExpect(jsonPath("$.paths['/api/v1/public/interactions/posts/{postId}/like'].post.security[0].bearerAuth")
						.isArray())
				.andExpect(jsonPath("$.components.schemas.PostInteractionResponse.properties.liked.type").value("boolean"))
				.andExpect(jsonPath("$.components.schemas.PostInteractionResponse.properties.favoritesCount.type").value("integer"))
				.andExpect(jsonPath("$.components.schemas.CommentInteractionResponse.properties.liked.type").value("boolean"))
				.andExpect(jsonPath("$.components.schemas.CommentInteractionResponse.properties.likesCount.type").value("integer"))
				.andExpect(jsonPath("$.paths['/api/v1/user/notifications'].get.responses['401']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/public/blog/posts'].get['x-response-envelope']")
						.value("ApiResponse<T>"));
	}
}
