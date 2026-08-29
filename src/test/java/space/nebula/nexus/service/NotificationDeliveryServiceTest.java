package space.nebula.nexus.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import space.nebula.nexus.common.event.NotificationEmailRequestedEvent;
import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.entity.NotificationDelivery;
import space.nebula.nexus.repository.NotificationDeliveryRepository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryServiceTest {
	@Mock
	private NotificationDeliveryRepository deliveryRepository;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@InjectMocks
	private NotificationDeliveryService notificationDeliveryService;

	@Test
	void queueEmailCreatesOneAuditableDeliveryAndPublishesAfterCommitEvent() {
		Notification notification = new Notification();
		notification.setId(12L);
		when(deliveryRepository.findByNotificationIdAndChannel(12L, "EMAIL")).thenReturn(Optional.empty());
		when(deliveryRepository.save(any(NotificationDelivery.class))).thenAnswer(invocation -> {
			NotificationDelivery delivery = invocation.getArgument(0);
			delivery.setId(31L);
			return delivery;
		});

		notificationDeliveryService.queueEmail(notification, "reader@example.com", "New reply", "A reply arrived.", "/post/example");

		ArgumentCaptor<NotificationDelivery> deliveryCaptor = ArgumentCaptor.forClass(NotificationDelivery.class);
		verify(deliveryRepository).save(deliveryCaptor.capture());
		assertEquals("QUEUED", deliveryCaptor.getValue().getStatus());
		assertEquals("EMAIL", deliveryCaptor.getValue().getChannel());
		verify(eventPublisher).publishEvent(new NotificationEmailRequestedEvent(31L, "reader@example.com", "New reply", "A reply arrived.", "/post/example"));
	}

	@Test
	void queueEmailDoesNotCreateDuplicatesForTheSameNotificationAndChannel() {
		Notification notification = new Notification();
		notification.setId(12L);
		when(deliveryRepository.findByNotificationIdAndChannel(12L, "EMAIL"))
				.thenReturn(Optional.of(new NotificationDelivery()));

		notificationDeliveryService.queueEmail(notification, "reader@example.com", "New reply", "A reply arrived.", null);

		verify(deliveryRepository, never()).save(any());
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	void failedDeliveryIsRecordedWithAnAttemptCount() {
		NotificationDelivery delivery = new NotificationDelivery();
		delivery.setAttempts(0);
		when(deliveryRepository.findById(31L)).thenReturn(Optional.of(delivery));

		notificationDeliveryService.markFailed(31L, new IllegalStateException("Mail gateway unavailable"));

		assertEquals("FAILED", delivery.getStatus());
		assertEquals(1, delivery.getAttempts());
		assertEquals("Mail gateway unavailable", delivery.getLastError());
		verify(deliveryRepository).save(eq(delivery));
	}

	@Test
	void staleFailedDeliveriesAreRequeuedFromTheDurableLog() {
		Notification notification = new Notification();
		notification.setTitle("New reply");
		notification.setContent("A reply arrived.");
		notification.setLink("/post/example");
		NotificationDelivery delivery = new NotificationDelivery();
		delivery.setId(31L);
		delivery.setRecipient("reader@example.com");
		delivery.setStatus("FAILED");
		delivery.setNotification(notification);
		when(deliveryRepository.findRetryableDeliveries(eq(List.of("FAILED")), any(), any()))
				.thenReturn(List.of(delivery));

		int retried = notificationDeliveryService.retryStaleEmailDeliveries(LocalDateTime.now().minusMinutes(10), 100);

		assertEquals(1, retried);
		assertEquals("QUEUED", delivery.getStatus());
		verify(eventPublisher).publishEvent(new NotificationEmailRequestedEvent(31L, "reader@example.com", "New reply", "A reply arrived.", "/post/example"));
	}

	@Test
	void failedDeliveryStopsRetryingAfterTheAttemptBudgetIsExhausted() {
		NotificationDelivery delivery = new NotificationDelivery();
		delivery.setAttempts(4);
		when(deliveryRepository.findById(31L)).thenReturn(Optional.of(delivery));

		notificationDeliveryService.markFailed(31L, new IllegalStateException("Mail gateway unavailable"));

		assertEquals(5, delivery.getAttempts());
		assertEquals("ABANDONED", delivery.getStatus());
		verify(deliveryRepository).save(delivery);
	}
}
