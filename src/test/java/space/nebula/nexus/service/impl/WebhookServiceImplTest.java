package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.entity.WebhookLog;
import space.nebula.nexus.repository.WebhookLogRepository;
import space.nebula.nexus.repository.WebhookRepository;
import space.nebula.nexus.security.OutboundUrlValidator;
import space.nebula.nexus.service.WebhookDeliveryClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceImplTest {

	@Mock
	private WebhookRepository webhookRepository;
	@Mock
	private WebhookLogRepository webhookLogRepository;
	@Mock
	private WebhookDeliveryClient deliveryClient;
	@Mock
	private OutboundUrlValidator outboundUrlValidator;

	@InjectMocks
	private WebhookServiceImpl webhookService;

	private Webhook testWebhook;
	private WebhookLog testWebhookLog;

	@BeforeEach
	void setUp() {
		testWebhook = new Webhook();
		testWebhook.setId(1L);
		testWebhook.setUrl("http://example.com/webhook");
		testWebhook.setSecret("test-secret");
		testWebhook.setIsActive(true);
		testWebhook.setIsDeleted(false);

		testWebhookLog = new WebhookLog();
		testWebhookLog.setId(100L);
		testWebhookLog.setWebhook(testWebhook);
		testWebhookLog.setDeliveryId("delivery-1");
		testWebhookLog.setEvent("POST_PUBLISHED");
		testWebhookLog.setRequestPayload("{\"postId\": 123}");
		testWebhookLog.setAttemptCount(1);
		testWebhookLog.setIsSuccess(false);
	}

	@Test
	void redeliverWebhookLog_Success() {
		when(webhookLogRepository.findByDeliveryId("delivery-1")).thenReturn(Optional.of(testWebhookLog));
		when(deliveryClient.post(eq("http://example.com/webhook"), eq("POST_PUBLISHED"), anyString(),
				eq("{\"postId\": 123}"))).thenReturn(new WebhookDeliveryClient.DeliveryResult(200, "ok", true));

		ApiResponse<Void> response = webhookService.redeliverWebhookLog("delivery-1");

		assertEquals(200, response.code());
		assertEquals(2, testWebhookLog.getAttemptCount());
		assertTrue(testWebhookLog.getIsSuccess());
		assertEquals(200, testWebhookLog.getResponseCode());
		assertNull(testWebhookLog.getErrorMessage());

		verify(outboundUrlValidator).validate("http://example.com/webhook");
		verify(webhookLogRepository).save(testWebhookLog);
	}

	@Test
	void redeliverWebhookLog_InactiveWebhook_ThrowsException() {
		testWebhook.setIsActive(false);
		when(webhookLogRepository.findByDeliveryId("delivery-1")).thenReturn(Optional.of(testWebhookLog));

		assertThrows(BusinessException.class, () -> webhookService.redeliverWebhookLog("delivery-1"));
		verifyNoInteractions(outboundUrlValidator, deliveryClient);
	}

	@Test
	void redeliverWebhookLog_DeletedWebhook_ThrowsException() {
		testWebhook.setIsDeleted(true);
		when(webhookLogRepository.findByDeliveryId("delivery-1")).thenReturn(Optional.of(testWebhookLog));

		assertThrows(BusinessException.class, () -> webhookService.redeliverWebhookLog("delivery-1"));
		verifyNoInteractions(outboundUrlValidator, deliveryClient);
	}

	@Test
	void redeliverWebhookLog_LogNotFound_ThrowsException() {
		when(webhookLogRepository.findByDeliveryId("invalid-id")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> webhookService.redeliverWebhookLog("invalid-id"));
		verifyNoInteractions(outboundUrlValidator, deliveryClient);
	}

	@Test
	void redeliverWebhookLog_DeliveryFails_SavesFailureAndThrowsException() {
		when(webhookLogRepository.findByDeliveryId("delivery-1")).thenReturn(Optional.of(testWebhookLog));
		when(deliveryClient.post(eq("http://example.com/webhook"), eq("POST_PUBLISHED"), anyString(),
				eq("{\"postId\": 123}")))
				.thenReturn(new WebhookDeliveryClient.DeliveryResult(500, "Internal Server Error", false));

		assertThrows(BusinessException.class, () -> webhookService.redeliverWebhookLog("delivery-1"));

		assertEquals(2, testWebhookLog.getAttemptCount());
		assertFalse(testWebhookLog.getIsSuccess());
		assertEquals(500, testWebhookLog.getResponseCode());

		verify(webhookLogRepository).save(testWebhookLog);
	}
}
