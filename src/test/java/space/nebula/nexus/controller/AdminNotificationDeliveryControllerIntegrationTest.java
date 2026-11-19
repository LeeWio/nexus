package space.nebula.nexus.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import space.nebula.nexus.payload.request.NotificationDeliveryRetryRequest;
import space.nebula.nexus.payload.response.NotificationContext;
import space.nebula.nexus.payload.response.NotificationDeliveryResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.NotificationDeliveryOperationsService;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.service.IInteractionService;
import space.nebula.nexus.security.OutboundUrlValidator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {space.nebula.nexus.config.MockRedisConfig.class,
		space.nebula.nexus.config.MockRabbitMQConfig.class}, properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminNotificationDeliveryControllerIntegrationTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private NotificationDeliveryOperationsService operations;

	@MockitoBean
	private PostSearchRepository postSearchRepository;

	@MockitoBean
	private IInteractionService interactionService;

	@MockitoBean
	private OutboundUrlValidator outboundUrlValidator;

	@Test
	void adminCanListAndInspectOperationalMetadataWithoutRecipientOrBody() throws Exception {
		NotificationDeliveryResponse delivery = delivery("FAILED", true);
		when(operations.list(eq(space.nebula.nexus.enums.NotificationDeliveryStatus.FAILED), eq(null), any()))
				.thenReturn(new PageResult<>(List.of(delivery), 1, 1, 20, 1));
		when(operations.detail(7L)).thenReturn(delivery);

		mockMvc.perform(get("/api/v1/admin/notifications/deliveries").param("status", "FAILED")
				.with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.list[0].id").value(7))
				.andExpect(jsonPath("$.data.list[0].status").value("FAILED"))
				.andExpect(jsonPath("$.data.list[0].recipient").doesNotExist())
				.andExpect(jsonPath("$.data.list[0].content").doesNotExist());

		mockMvc.perform(get("/api/v1/admin/notifications/deliveries/7").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.retryable").value(true));
		verify(operations).list(eq(space.nebula.nexus.enums.NotificationDeliveryStatus.FAILED), eq(null), any());
	}

	@Test
	void adminRetryUsesTheAuditedRequestAndReturnsUpdatedDelivery() throws Exception {
		UUID requestId = UUID.fromString("00000000-0000-0000-0000-000000000007");
		NotificationDeliveryResponse response = delivery("QUEUED", false);
		when(operations.retry(eq(7L), any(NotificationDeliveryRetryRequest.class))).thenReturn(response);

		mockMvc.perform(post("/api/v1/admin/notifications/deliveries/7/retry").with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
						new NotificationDeliveryRetryRequest(requestId, "Provider health check passed."))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("QUEUED"));
		verify(operations).retry(eq(7L),
				eq(new NotificationDeliveryRetryRequest(requestId, "Provider health check passed.")));
	}

	@Test
	void nonAdminCannotAccessDeliveryOperations() throws Exception {
		mockMvc.perform(get("/api/v1/admin/notifications/deliveries").with(user("reader").roles("USER")))
				.andExpect(status().isForbidden());
	}

	private NotificationDeliveryResponse delivery(String status, boolean retryable) {
		return new NotificationDeliveryResponse(7L, 4L, "EMAIL",
				space.nebula.nexus.enums.NotificationDeliveryStatus.valueOf(status), 2, 5,
				"FAILED".equals(status) ? "SMTP timeout" : null, LocalDateTime.now(), null, LocalDateTime.now(),
				retryable, new NotificationContext(NotificationContext.ObjectType.POST, 4L, null,
						NotificationContext.Action.VIEW));
	}
}
