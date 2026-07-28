package space.nebula.nexus.listener;

import cn.hutool.core.lang.Dict;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.entity.WebhookLog;
import space.nebula.nexus.payload.request.WebhookMessage;
import space.nebula.nexus.repository.WebhookLogRepository;
import space.nebula.nexus.repository.WebhookRepository;
import space.nebula.nexus.security.OutboundUrlValidator;
import space.nebula.nexus.service.WebhookDeliveryClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookMessageListenerTest {

	@Mock
	private WebhookRepository webhookRepository;
	@Mock
	private WebhookLogRepository webhookLogRepository;
	@Mock
	private OutboundUrlValidator outboundUrlValidator;
	@Mock
	private WebhookDeliveryClient deliveryClient;

	@InjectMocks
	private WebhookMessageListener webhookMessageListener;

	private Webhook testWebhook;

	@BeforeEach
	void setUp() {
		testWebhook = new Webhook();
		testWebhook.setId(1L);
		testWebhook.setUrl("http://example.com/webhook");
		testWebhook.setSecret("test-secret");
		testWebhook.setIsActive(true);
	}

	@Test
	void handleWebhookDispatch_Success() {
		WebhookMessage message = new WebhookMessage(1L, "POST_PUBLISHED", Dict.create().set("postId", 123));

		when(webhookRepository.findById(1L)).thenReturn(Optional.of(testWebhook));
		when(deliveryClient.post(anyString(), anyString(), anyString(), anyString()))
				.thenReturn(new WebhookDeliveryClient.DeliveryResult(200, "ok", true));

		webhookMessageListener.handleWebhookDispatch(message);

		org.mockito.ArgumentCaptor<WebhookLog> logCaptor = org.mockito.ArgumentCaptor.forClass(WebhookLog.class);
		verify(webhookLogRepository).save(logCaptor.capture());
		assertEquals(1, logCaptor.getValue().getAttemptCount());
	}

	@Test
	void handleWebhookDispatch_Inactive_Skip() {
		testWebhook.setIsActive(false);
		WebhookMessage message = new WebhookMessage(1L, "POST_PUBLISHED", Dict.create().set("postId", 123));

		when(webhookRepository.findById(1L)).thenReturn(Optional.of(testWebhook));

		webhookMessageListener.handleWebhookDispatch(message);

		verify(webhookLogRepository, never()).save(any(WebhookLog.class));
	}

	@Test
	void handleWebhookDispatch_AlreadySuccessful_Skip() {
		WebhookMessage message = new WebhookMessage(1L, "delivery-1", "POST_PUBLISHED",
				Dict.create().set("postId", 123));
		WebhookLog existingLog = new WebhookLog();
		existingLog.setDeliveryId("delivery-1");
		existingLog.setIsSuccess(true);

		when(webhookRepository.findById(1L)).thenReturn(Optional.of(testWebhook));
		when(webhookLogRepository.findByDeliveryId("delivery-1")).thenReturn(Optional.of(existingLog));

		webhookMessageListener.handleWebhookDispatch(message);

		verifyNoInteractions(outboundUrlValidator, deliveryClient);
		verify(webhookLogRepository, never()).save(any(WebhookLog.class));
	}
}
