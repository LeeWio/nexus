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
import java.util.Optional;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class NotificationDeliveryService {
	private static final int DELIVERY_LEASE_MINUTES = 10;
	private final NotificationDeliveryRepository deliveryRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public void queueEmail(Notification notification, String recipient, String title, String content, String link) {
		if (recipient == null || recipient.isBlank() || notification.getId() == null)
			return;
		queue(notification.getId(), recipient, title, content, link, "NOTIFICATION:" + notification.getId());
	}

	@Transactional
	public void queueExternalEmail(String recipient, String title, String content, String link, String eventKey) {
		if (recipient == null || recipient.isBlank())
			return;
		queue(null, recipient, title, content, link, "EXTERNAL:" + eventKey);
	}

	private void queue(Long notificationId, String recipient, String title, String content, String link, String key) {
		if (key.length() > 150)
			throw new IllegalArgumentException("Delivery event key is too long");
		if (deliveryRepository.findByDeduplicationKey(key).isPresent())
			return;
		deliveryRepository.insertEmailOnce(notificationId, recipient, title, content, link, key,
				LocalDateTime.now().plusMinutes(DELIVERY_LEASE_MINUTES));
		deliveryRepository.findByDeduplicationKey(key).ifPresent(this::publish);
	}

	/**
	 * Claims one SMTP attempt under a row lock; repeated or delayed broker messages
	 * become no-ops.
	 */
	@Transactional
	public Optional<DeliveryClaim> beginDelivery(Long deliveryId) {
		if (deliveryId == null)
			return Optional.empty();
		return deliveryRepository.findForUpdate(deliveryId).flatMap(delivery -> {
			if (!"QUEUED".equals(delivery.getStatus()))
				return Optional.empty();
			if (currentAttempts(delivery) >= delivery.getMaxAttempts()) {
				delivery.setStatus("ABANDONED");
				return Optional.empty();
			}
			delivery.setStatus("SENDING");
			delivery.setAttempts(currentAttempts(delivery) + 1);
			delivery.setNextAttemptAt(LocalDateTime.now().plusMinutes(DELIVERY_LEASE_MINUTES));
			return Optional.of(new DeliveryClaim(delivery.getId(), delivery.getAttempts(), delivery.getRecipient(),
					delivery.getTitle(), delivery.getContent(), delivery.getLink()));
		});
	}

	@Transactional
	public void markDelivered(Long deliveryId, int attempt) {
		deliveryRepository.findForUpdate(deliveryId).ifPresent(delivery -> {
			if (!ownsAttempt(delivery, attempt))
				return;
			delivery.setStatus("DELIVERED");
			delivery.setDeliveredAt(LocalDateTime.now());
			delivery.setLastError(null);
		});
	}

	@Transactional
	public void markFailed(Long deliveryId, int attempt, Exception error) {
		deliveryRepository.findForUpdate(deliveryId).ifPresent(delivery -> {
			if (ownsAttempt(delivery, attempt))
				fail(delivery, error);
		});
	}

	@Transactional
	public void markEnqueueFailed(Long deliveryId, Exception error) {
		markEnqueueFailed(deliveryId, 0, error);
	}

	@Transactional
	public void markEnqueueFailed(Long deliveryId, int generation, Exception error) {
		deliveryRepository.findForUpdate(deliveryId).ifPresent(delivery -> {
			if (!"QUEUED".equals(delivery.getStatus()) || delivery.getEnqueueGeneration() != generation)
				return;
			delivery.setAttempts(currentAttempts(delivery) + 1);
			fail(delivery, error);
		});
	}

	@Transactional
	public int retryStaleEmailDeliveries(LocalDateTime before, int limit) {
		List<NotificationDelivery> deliveries = deliveryRepository
				.findRetryableDeliveries(List.of("FAILED", "QUEUED", "SENDING"), before, PageRequest.of(0, limit));
		int retried = 0;
		for (NotificationDelivery delivery : deliveries) {
			if (currentAttempts(delivery) >= delivery.getMaxAttempts()) {
				delivery.setStatus("ABANDONED");
				continue;
			}
			requeue(delivery);
			retried++;
		}
		return retried;
	}

	/**
	 * Called with a managed row already locked by the recovery or manual retry
	 * transaction.
	 */
	public void requeue(NotificationDelivery delivery) {
		delivery.setStatus("QUEUED");
		delivery.setEnqueueGeneration(Math.incrementExact(delivery.getEnqueueGeneration()));
		delivery.setNextAttemptAt(LocalDateTime.now().plusMinutes(DELIVERY_LEASE_MINUTES));
		publish(delivery);
	}

	private boolean ownsAttempt(NotificationDelivery delivery, int attempt) {
		return "SENDING".equals(delivery.getStatus()) && currentAttempts(delivery) == attempt;
	}

	private void fail(NotificationDelivery delivery, Exception error) {
		int attempts = currentAttempts(delivery);
		delivery.setStatus(attempts >= delivery.getMaxAttempts() ? "ABANDONED" : "FAILED");
		delivery.setNextAttemptAt(LocalDateTime.now().plusMinutes(Math.min(60, 1L << Math.min(attempts, 6))));
		// Store only an error class; SMTP exception text can contain addresses or
		// credentials.
		delivery.setLastError(error.getClass().getSimpleName());
	}

	private void publish(NotificationDelivery delivery) {
		eventPublisher.publishEvent(new NotificationEmailRequestedEvent(delivery.getId(), delivery.getRecipient(),
				delivery.getTitle(), delivery.getContent(), delivery.getLink(), delivery.getEnqueueGeneration()));
	}

	private int currentAttempts(NotificationDelivery delivery) {
		return delivery.getAttempts() == null ? 0 : delivery.getAttempts();
	}

	public record DeliveryClaim(Long id, int attempt, String recipient, String title, String content, String link) {
	}
}
