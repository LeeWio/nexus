package space.nebula.nexus.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.entity.WebhookLog;
import space.nebula.nexus.repository.WebhookLogRepository;
import space.nebula.nexus.repository.WebhookRepository;
import space.nebula.nexus.security.OutboundUrlValidator;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.IInteractionService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

	@Autowired
	private WebhookRepository webhookRepository;

	@Autowired
	private WebhookLogRepository webhookLogRepository;

	@MockitoBean
	private OutboundUrlValidator outboundUrlValidator;

	@Test
	void sitemapUsesAnEvaluableCacheKeyAndReturnsXml() throws Exception {
		mockMvc.perform(get("/api/v1/public/seo/sitemap.xml")).andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
				.andExpect(content().string(containsString("/single")));
	}

	@Test
	@Transactional
	void webhookEditsPreserveSecretsAndDeliveryHistoryReturnsOnlyMetadata() throws Exception {
		Webhook webhook = new Webhook();
		webhook.setName("Fixture");
		webhook.setUrl("https://example.test/webhook");
		webhook.setSecret("fixture-secret");
		webhook.setEvents("POST_PUBLISHED");
		webhook.setCreatedAt(LocalDateTime.now());
		Long id = webhookRepository.saveAndFlush(webhook).getId();
		String endpoint = "/api/v1/admin/webhooks/" + id;

		mockMvc.perform(
				put(endpoint).with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(
						"""
								{"name":"Edited","url":"https://example.test/webhook","events":["MOMENT_PUBLISHED"],"isActive":true}
								"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.name").value("Edited"))
				.andExpect(jsonPath("$.data.secret").doesNotExist());
		assertEquals("fixture-secret", webhookRepository.findById(id).orElseThrow().getSecret());

		mockMvc.perform(post(endpoint + "/test").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deliveryId").isNotEmpty());

		WebhookLog log = new WebhookLog();
		log.setWebhook(webhook);
		log.setEvent("ping");
		log.setDeliveryId("delivery-fixture");
		log.setRequestPayload("test-payload");
		log.setResponsePayload("test-response");
		log.setResponseCode(200);
		log.setIsSuccess(true);
		log.setCreatedAt(LocalDateTime.now());
		webhookLogRepository.saveAndFlush(log);

		mockMvc.perform(get(endpoint + "/logs").with(user("reader").roles("USER"))).andExpect(status().isForbidden());
		mockMvc.perform(get(endpoint + "/logs").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.list[0].deliveryId").value("delivery-fixture"))
				.andExpect(jsonPath("$.data.list[0].webhook").doesNotExist())
				.andExpect(jsonPath("$.data.list[0].requestPayload").doesNotExist())
				.andExpect(jsonPath("$.data.list[0].responsePayload").doesNotExist());
	}

	@Test
	void publishesConsistentFrontendContractAndAuthenticationMetadata() throws Exception {
		MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.info.description").value(containsString("Frontend integration contract")))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.schemas.ApiError.properties.traceId").exists())
				.andExpect(jsonPath("$.paths['/api/v1/public/blog/posts'].get.security").doesNotExist())
				.andExpect(jsonPath("$.paths['/api/v1/user/notifications'].get.security[0].bearerAuth").isArray())
				.andExpect(jsonPath(
						"$.paths['/api/v1/public/interactions/posts/{postId}/like'].post.security[0].bearerAuth")
						.isArray())
				.andExpect(
						jsonPath("$.paths['/api/v1/user/library/reading-list'].get.security[0].bearerAuth").isArray())
				.andExpect(jsonPath("$.paths['/api/v1/user/library/reading-list/{postId}'].put.security[0].bearerAuth")
						.isArray())
				.andExpect(
						jsonPath("$.paths['/api/v1/user/library/reading-list/{postId}'].delete.security[0].bearerAuth")
								.isArray())
				.andExpect(
						jsonPath("$.components.schemas.PostResponse.properties.isInReadingList.type").value("boolean"))
				.andExpect(
						jsonPath("$.components.schemas.PostInteractionResponse.properties.liked.type").value("boolean"))
				.andExpect(jsonPath("$.components.schemas.PostInteractionResponse.properties.favoritesCount.type")
						.value("integer"))
				.andExpect(jsonPath("$.components.schemas.CommentInteractionResponse.properties.liked.type")
						.value("boolean"))
				.andExpect(jsonPath("$.components.schemas.CommentInteractionResponse.properties.likesCount.type")
						.value("integer"))
				.andExpect(jsonPath("$.paths['/api/v1/user/notifications'].get.responses['401']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/public/blog/posts'].get['x-response-envelope']")
						.value("ApiResponse<T>"))
				.andReturn();

		String output = System.getProperty("nexus.openapi.output");
		if (output != null) {
			Files.write(Path.of(output), result.getResponse().getContentAsByteArray());
		}
	}
}
