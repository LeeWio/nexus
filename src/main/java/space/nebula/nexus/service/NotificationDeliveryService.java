package space.nebula.nexus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.event.NotificationEmailRequestedEvent;
import space.nebula.nexus.entity.Notification;
import space.nebula.nexus.entity.NotificationDelivery;
import space.nebula.nexus.repository.NotificationDeliveryRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {
	private static final String EMAIL = "EMAIL";
	private static final int MAX_EMAIL_ATTEMPTS = 5;
	private final NotificationDeliveryRepository deliveryRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public void queueEmail(Notification notification, String recipient, String title, String content, String link) {
		if (recipient == null || recipient.isBlank() || notification.getId() == null) return;
		if (deliveryRepository.findByNotificationIdAndChannel(notification.getId(), EMAIL).isPresent()) return;
		NotificationDelivery delivery = new NotificationDelivery();
		delivery.setNotification(notification);
		delivery.setChannel(EMAIL);
		delivery.setRecipient(recipient);
		delivery.setStatus("QUEUED");
		delivery = deliveryRepository.save(delivery);
		eventPublisher.publishEvent(new NotificationEmailRequestedEvent(delivery.getId(), recipient, title, content, link));
	}

	@Transactional
	public void markDelivered(Long deliveryId) {
		if (deliveryId == null) return;
		deliveryRepository.findById(deliveryId).ifPresent(delivery -> {
			delivery.setStatus("DELIVERED");
			delivery.setDeliveredAt(LocalDateTime.now());
			delivery.setLastError(null);
			delivery.setAttempts(currentAttempts(delivery) + 1);
			deliveryRepository.save(delivery);
		});
	}

	@Transactional
	public void markFailed(Long deliveryId, Exception error) {
		if (deliveryId == null) return;
		deliveryRepository.findById(deliveryId).ifPresent(delivery -> {
			int attempts = currentAttempts(delivery) + 1;
			delivery.setAttempts(attempts);
			delivery.setStatus(attempts >= MAX_EMAIL_ATTEMPTS ? "ABANDONED" : "FAILED");
			delivery.setLastError(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage().substring(0, Math.min(1000, error.getMessage().length())));
			deliveryRepository.save(delivery);
		});
	}

	@Transactional
	public int retryStaleEmailDeliveries(LocalDateTime before, int limit) {
		List<NotificationDelivery> deliveries = deliveryRepository.findRetryableDeliveries(List.of("FAILED"),
				before, org.springframework.data.domain.PageRequest.of(0, limit));
		for (NotificationDelivery delivery : deliveries) {
			delivery.setStatus("QUEUED");
			eventPublisher.publishEvent(new NotificationEmailRequestedEvent(delivery.getId(), delivery.getRecipient(),
					delivery.getNotification().getTitle(), delivery.getNotification().getContent(), delivery.getNotification().getLink()));
		}
		return deliveries.size();
	}

	private int currentAttempts(NotificationDelivery delivery) {
		return delivery.getAttempts() == null ? 0 : delivery.getAttempts();
	}
}
