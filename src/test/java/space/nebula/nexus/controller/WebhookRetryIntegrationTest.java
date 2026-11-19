package space.nebula.nexus.controller;

import cn.hutool.core.lang.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import space.nebula.nexus.config.MockRabbitMQConfig;
import space.nebula.nexus.config.MockRedisConfig;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.entity.WebhookLog;
import space.nebula.nexus.repository.WebhookLogRepository;
import space.nebula.nexus.repository.WebhookRepository;
import space.nebula.nexus.repository.search.PostSearchRepository;
import space.nebula.nexus.security.OutboundUrlValidator;
import space.nebula.nexus.service.WebhookDeliveryClient;
import space.nebula.nexus.service.WebhookDeliveryClient.DeliveryResult;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = {MockRedisConfig.class, MockRabbitMQConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebhookRetryIntegrationTest {

	private static final String TARGET = "https://example.test/webhook";
	private static final String PAYLOAD = "{\"postId\":123}";
	private static final String EVENT = "POST_PUBLISHED";
	private static final String SIGNATURE = "f8c6bceb1040d932c9948826351a5e9d2c363333b4b4aa9dd46e52e9873c69df";

	@Autowired
	private MockMvc mvc;
	@Autowired
	private WebhookRepository webhooks;
	@Autowired
	private WebhookLogRepository logs;
	@MockitoBean
	private PostSearchRepository search;
	@MockitoBean
	private OutboundUrlValidator urlValidator;
	@MockitoBean
	private WebhookDeliveryClient deliveryClient;

	private Webhook webhook;
	private WebhookLog entry;
	private String endpoint;

	@BeforeEach
	void createFailedDelivery() {
		webhook = new Webhook();
		webhook.setName("Retry fixture");
		webhook.setUrl(TARGET);
		webhook.setSecret("fixture-secret");
		webhook.setEvents(EVENT);
		webhook = webhooks.saveAndFlush(webhook);

		entry = new WebhookLog();
		entry.setDeliveryId(UUID.fastUUID().toString(true));
		entry.setWebhook(webhook);
		entry.setEvent(EVENT);
		entry.setRequestPayload(PAYLOAD);
		entry.setIsSuccess(false);
		entry.setResponseCode(503);
		entry = logs.saveAndFlush(entry);
		endpoint = "/api/v1/admin/webhooks/logs/" + entry.getDeliveryId() + "/redeliver";
	}

	@AfterEach
	void removeFixture() {
		logs.deleteById(entry.getId());
		webhooks.deleteById(webhook.getId());
	}

	@Test
	void recordsReceiverFailureAndRejectsDuplicateRetryWhileClaimedOrCompleted() throws Exception {
		var started = new CountDownLatch(1);
		var finish = new CountDownLatch(1);
		when(deliveryClient.post(TARGET, EVENT, SIGNATURE, PAYLOAD, entry.getDeliveryId())).thenAnswer(call -> {
			started.countDown();
			assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
			assertTrue(finish.await(10, TimeUnit.SECONDS));
			return new DeliveryResult(502, "upstream failure", false);
		});
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> mvc.perform(retry(1)).andExpect(status().isOk())
					.andExpect(jsonPath("$.data.attemptCount").value(2))
					.andExpect(jsonPath("$.data.responseCode").value(502))
					.andExpect(jsonPath("$.data.isSuccess").value(false)).andReturn());
			try {
				assertTrue(started.await(10, TimeUnit.SECONDS));
				mvc.perform(retry(1)).andExpect(status().isConflict());
				mvc.perform(retry(2)).andExpect(status().isConflict());
				mvc.perform(
						get("/api/v1/admin/webhooks/" + webhook.getId() + "/logs").with(user("admin").roles("ADMIN")))
						.andExpect(status().isOk()).andExpect(jsonPath("$.data.list[0].inProgress").value(true));
			} finally {
				finish.countDown();
			}
			first.get(10, TimeUnit.SECONDS);
		}
		mvc.perform(retry(1)).andExpect(status().isConflict());
		verify(deliveryClient).post(TARGET, EVENT, SIGNATURE, PAYLOAD, entry.getDeliveryId());
		assertEquals(502, logs.findById(entry.getId()).orElseThrow().getResponseCode());
	}

	@Test
	void successfulDeliveryCannotBeRetriedAgain() throws Exception {
		when(deliveryClient.post(TARGET, EVENT, SIGNATURE, PAYLOAD, entry.getDeliveryId()))
				.thenReturn(new DeliveryResult(204, "", true));
		mvc.perform(retry(1)).andExpect(status().isOk()).andExpect(jsonPath("$.data.isSuccess").value(true));
		mvc.perform(retry(2)).andExpect(status().isConflict());
		verify(deliveryClient).post(TARGET, EVENT, SIGNATURE, PAYLOAD, entry.getDeliveryId());
	}

	@Test
	void rejectsUnauthorizedAndPausedWebhookRetries() throws Exception {
		mvc.perform(post(endpoint).param("attemptCount", "1")).andExpect(status().isUnauthorized());
		mvc.perform(post(endpoint).param("attemptCount", "1").with(user("reader").roles("USER")))
				.andExpect(status().isForbidden());
		webhook.setIsActive(false);
		webhooks.saveAndFlush(webhook);
		mvc.perform(retry(1)).andExpect(status().isBadRequest());
		verifyNoInteractions(deliveryClient);
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder retry(int count) {
		return post(endpoint).param("attemptCount", Integer.toString(count)).with(user("admin").roles("ADMIN"));
	}
}
