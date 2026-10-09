package space.nebula.nexus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import space.nebula.nexus.common.event.NotificationEmailRequestedEvent;
import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.entity.NotificationDelivery;
import space.nebula.nexus.repository.NotificationDeliveryRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {
	@Mock
	private NotificationDeliveryRepository deliveryRepository;
	@Mock
	private ApplicationEventPublisher eventPublisher;
	@InjectMocks
	private NotificationDeliveryService service;

	@Test
	void queueCreatesImmutablePayloadAndOneDeliveryPerNotification() {
		Notification notification = new Notification();
		notification.setId(12L);
		NotificationDelivery delivery = delivery("QUEUED", 0);
		when(deliveryRepository.findByDeduplicationKey("NOTIFICATION:12")).thenReturn(Optional.empty(),
				Optional.of(delivery));

		service.queueEmail(notification, "reader@example.com", "New reply", "A reply arrived.", "/post/example");

		verify(deliveryRepository).insertEmailOnce(eq(12L), eq("reader@example.com"), eq("New reply"),
				eq("A reply arrived."), eq("/post/example"), eq("NOTIFICATION:12"), any());
		verify(eventPublisher).publishEvent(new NotificationEmailRequestedEvent(31L, "reader@example.com", "New reply",
				"A reply arrived.", "/post/example"));
	}

	@Test
	void replayDoesNotEnqueueAnotherEmail() {
		when(deliveryRepository.findByDeduplicationKey("EXTERNAL:FRIEND:1"))
				.thenReturn(Optional.of(delivery("DELIVERED", 1)));
		service.queueExternalEmail("reader@example.com", "Result", "Approved", null, "FRIEND:1");
		verifyNoInteractions(eventPublisher);
		verify(deliveryRepository, never()).insertEmailOnce(any(), any(), any(), any(), any(), any(), any());
	}

	@Test
	void duplicateBrokerMessagesCannotClaimAnActiveAttempt() {
		NotificationDelivery delivery = delivery("QUEUED", 0);
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		assertEquals(1, service.beginDelivery(31L).orElseThrow().attempt());
		assertTrue(service.beginDelivery(31L).isEmpty());
		assertEquals("SENDING", delivery.getStatus());
		assertEquals(1, delivery.getAttempts());
	}

	@Test
	void failureRetainsAttemptAndSchedulesBackoffWithoutSensitiveExceptionText() {
		NotificationDelivery delivery = delivery("SENDING", 2);
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		LocalDateTime before = LocalDateTime.now();
		service.markFailed(31L, 2, new IllegalStateException("secret SMTP credentials"));
		assertEquals("FAILED", delivery.getStatus());
		assertEquals(2, delivery.getAttempts());
		assertEquals("IllegalStateException", delivery.getLastError());
		assertTrue(delivery.getNextAttemptAt().isAfter(before.plusMinutes(3)));
	}

	@Test
	void fifthFailureAbandonsDelivery() {
		NotificationDelivery delivery = delivery("SENDING", 5);
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		service.markFailed(31L, 5, new IllegalStateException());
		assertEquals("ABANDONED", delivery.getStatus());
	}

	@Test
	void expiredQueuedSendingAndFailedRecordsAreRecoverableUsingSnapshots() {
		List<NotificationDelivery> deliveries = List.of(delivery("QUEUED", 0), delivery("SENDING", 1),
				delivery("FAILED", 2));
		when(deliveryRepository.findRetryableDeliveries(eq(List.of("FAILED", "QUEUED", "SENDING")), any(), any()))
				.thenReturn(deliveries);
		assertEquals(3, service.retryStaleEmailDeliveries(LocalDateTime.now(), 100));
		for (NotificationDelivery delivery : deliveries)
			assertEquals("QUEUED", delivery.getStatus());
		verify(eventPublisher, times(3)).publishEvent(any(NotificationEmailRequestedEvent.class));
	}

	@Test
	void finalExpiredAttemptBecomesAbandonedWithoutAnotherSend() {
		NotificationDelivery delivery = delivery("SENDING", 5);
		when(deliveryRepository.findRetryableDeliveries(anyList(), any(), any())).thenReturn(List.of(delivery));
		assertEquals(0, service.retryStaleEmailDeliveries(LocalDateTime.now(), 100));
		assertEquals("ABANDONED", delivery.getStatus());
		verifyNoInteractions(eventPublisher);
	}

	@Test
	void lateCallbacksCannotOverwriteNewAttemptsOrDeliveredState() {
		NotificationDelivery delivery = delivery("SENDING", 3);
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		service.markDelivered(31L, 2);
		service.markFailed(31L, 2, new IllegalStateException());
		assertEquals("SENDING", delivery.getStatus());
		service.markDelivered(31L, 3);
		service.markFailed(31L, 3, new IllegalStateException());
		service.markEnqueueFailed(31L, new IllegalStateException());
		assertEquals("DELIVERED", delivery.getStatus());
		assertNotNull(delivery.getDeliveredAt());
	}

	@Test
	void brokerFailureConsumesOneAttemptAndIsRecoverable() {
		NotificationDelivery delivery = delivery("QUEUED", 0);
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		service.markEnqueueFailed(31L, new IllegalStateException());
		assertEquals("FAILED", delivery.getStatus());
		assertEquals(1, delivery.getAttempts());
	}

	@Test
	void staleEnqueueFailureCannotMarkANewerGenerationFailed() {
		NotificationDelivery delivery = delivery("QUEUED", 0);
		delivery.setEnqueueGeneration(2);
		when(deliveryRepository.findForUpdate(31L)).thenReturn(Optional.of(delivery));
		service.markEnqueueFailed(31L, 1, new IllegalStateException("old publish"));
		assertEquals("QUEUED", delivery.getStatus());
		assertEquals(0, delivery.getAttempts());
		service.markEnqueueFailed(31L, 2, new IllegalStateException("current publish"));
		assertEquals("FAILED", delivery.getStatus());
		assertEquals(1, delivery.getAttempts());
	}

	private NotificationDelivery delivery(String status, int attempts) {
		NotificationDelivery delivery = new NotificationDelivery();
		delivery.setId(31L);
		delivery.setStatus(status);
		delivery.setAttempts(attempts);
		delivery.setRecipient("reader@example.com");
		delivery.setTitle("New reply");
		delivery.setContent("A reply arrived.");
		delivery.setLink("/post/example");
		return delivery;
	}
}
