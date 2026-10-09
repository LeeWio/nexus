package space.nebula.nexus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.entity.NotificationDelivery;
import space.nebula.nexus.entity.NotificationDeliveryRetry;
import space.nebula.nexus.payload.request.NotificationDeliveryRetryRequest;
import space.nebula.nexus.repository.NotificationDeliveryRepository;
import space.nebula.nexus.repository.NotificationDeliveryRetryRepository;
import space.nebula.nexus.security.util.SecurityUtil;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryOperationsServiceTest {
	@Mock
	private NotificationDeliveryRepository deliveryRepository;
	@Mock
	private NotificationDeliveryRetryRepository retryRepository;
	@Mock
	private NotificationDeliveryService deliveryService;
	@InjectMocks
	private NotificationDeliveryOperationsService service;

	@Test
	void manualRetryKeepsAttemptCountRaisesBudgetAndAuditsOperator() {
		NotificationDelivery delivery = failedDelivery(5, 5);
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		when(retryRepository.existsByDeliveryIdAndRequestId(eq(31L), anyString())).thenReturn(false);
		UUID requestId = UUID.randomUUID();

		try (MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
			security.when(SecurityUtil::getCurrentUsername).thenReturn("moderator");
			var response = service.retry(31L, new NotificationDeliveryRetryRequest(requestId, " SMTP recovered "));
			assertEquals(5, response.attempts());
			assertEquals(10, response.maxAttempts());
			assertTrue(response.retryable());
		}

		ArgumentCaptor<NotificationDeliveryRetry> audit = ArgumentCaptor.forClass(NotificationDeliveryRetry.class);
		verify(retryRepository).save(audit.capture());
		assertEquals("FAILED", audit.getValue().getPreviousStatus());
		assertEquals(5, audit.getValue().getPreviousMaxAttempts());
		assertEquals(10, audit.getValue().getNewMaxAttempts());
		assertEquals(5, audit.getValue().getAttemptsAtRetry());
		assertEquals("moderator", audit.getValue().getRequestedBy());
		assertEquals("SMTP recovered", audit.getValue().getReason());
		verify(deliveryService).requeue(delivery);
		assertEquals(10, delivery.getMaxAttempts());
	}

	@Test
	void repeatedRequestIdDoesNotGrantAnotherBudget() {
		NotificationDelivery delivery = failedDelivery(5, 10);
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		when(retryRepository.existsByDeliveryIdAndRequestId(31L, "11111111-1111-1111-1111-111111111111"))
				.thenReturn(true);

		try (MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
			security.when(SecurityUtil::getCurrentUsername).thenReturn("moderator");
			var response = service.retry(31L, new NotificationDeliveryRetryRequest(
					UUID.fromString("11111111-1111-1111-1111-111111111111"), "retry"));
			assertEquals(10, response.maxAttempts());
		}

		verify(retryRepository, never()).save(any());
		verifyNoInteractions(deliveryService);
	}

	@Test
	void queuedDeliveryCannotBeManuallyRetried() {
		NotificationDelivery delivery = failedDelivery(1, 5);
		delivery.setStatus("QUEUED");
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		when(retryRepository.existsByDeliveryIdAndRequestId(eq(31L), anyString())).thenReturn(false);

		try (MockedStatic<SecurityUtil> security = mockStatic(SecurityUtil.class)) {
			security.when(SecurityUtil::getCurrentUsername).thenReturn("moderator");
			BusinessException error = assertThrows(BusinessException.class,
					() -> service.retry(31L, new NotificationDeliveryRetryRequest(UUID.randomUUID(), "still queued")));
			assertEquals(409, error.getCode());
		}
		verifyNoInteractions(deliveryService);
	}

	@Test
	void oversizedPageIsRejected() {
		BusinessException error = assertThrows(BusinessException.class,
				() -> service.list(null, null, PageRequest.of(0, 101)));
		assertEquals(400, error.getCode());
	}

	private NotificationDelivery failedDelivery(int attempts, int maxAttempts) {
		NotificationDelivery delivery = new NotificationDelivery();
		delivery.setId(31L);
		delivery.setStatus("FAILED");
		delivery.setAttempts(attempts);
		delivery.setMaxAttempts(maxAttempts);
		delivery.setEnqueueGeneration(1);
		delivery.setChannel("EMAIL");
		return delivery;
	}
}
