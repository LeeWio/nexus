package space.nebula.nexus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.entity.NewsletterDelivery;
import space.nebula.nexus.entity.NewsletterDeliveryBatch;
import space.nebula.nexus.entity.Subscriber;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.repository.NewsletterDeliveryBatchRepository;
import space.nebula.nexus.repository.NewsletterDeliveryRepository;
import space.nebula.nexus.payload.response.NewsletterDeliveryBatchResponse;
import space.nebula.nexus.payload.response.NewsletterDeliveryResponse;
import space.nebula.nexus.payload.response.PageResult;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Maintains durable newsletter delivery state separate from notification mail.
 */
@Service
@RequiredArgsConstructor
public class NewsletterDeliveryService {
	private static final int MAX_DELIVERY_ATTEMPTS = 5;
	private final NewsletterDeliveryBatchRepository batchRepository;
	private final NewsletterDeliveryRepository deliveryRepository;
	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	@Transactional
	public NewsletterDeliveryBatch startBatch() {
		NewsletterDeliveryBatch batch = new NewsletterDeliveryBatch();
		batch.setStatus("QUEUING");
		batch.setStartedAt(LocalDateTime.now());
		return batchRepository.save(batch);
	}

	@Transactional
	public NewsletterDelivery queue(NewsletterDeliveryBatch batch, Subscriber subscriber, TemplateMailMessage message) {
		NewsletterDelivery delivery = new NewsletterDelivery();
		delivery.setBatch(batch);
		delivery.setSubscriber(subscriber);
		delivery.setStatus("QUEUED");
		delivery.setRecipient(message.getTo());
		delivery.setSubject(message.getSubject());
		delivery.setTemplateName(message.getTemplateName());
		delivery.setTemplateVariables(serializeVariables(message.getVariables()));
		delivery = deliveryRepository.save(delivery);
		batch.setRecipientCount(batch.getRecipientCount() + 1);
		batch.setQueuedCount(batch.getQueuedCount() + 1);
		batchRepository.save(batch);
		return delivery;
	}

	@Transactional
	public void completeQueueing(NewsletterDeliveryBatch batch) {
		batch.setStatus(batch.getQueuedCount() == 0 ? "SKIPPED" : "PROCESSING");
		if (batch.getQueuedCount() == 0)
			batch.setCompletedAt(LocalDateTime.now());
		batchRepository.save(batch);
	}

	@Transactional
	public void markDelivered(Long deliveryId) {
		if (deliveryId == null)
			return;
		deliveryRepository.findById(deliveryId).ifPresent(delivery -> {
			if ("DELIVERED".equals(delivery.getStatus()))
				return;
			delivery.setStatus("DELIVERED");
			delivery.setDeliveredAt(LocalDateTime.now());
			delivery.setLastError(null);
			delivery.setAttempts(currentAttempts(delivery) + 1);
			deliveryRepository.save(delivery);
			updateBatch(delivery.getBatch());
		});
	}

	@Transactional
	public void markFailed(Long deliveryId, Exception error) {
		if (deliveryId == null)
			return;
		deliveryRepository.findById(deliveryId).ifPresent(delivery -> {
			if ("DELIVERED".equals(delivery.getStatus()))
				return;
			recordFailure(delivery, error);
		});
	}

	@Transactional
	public int retryStaleDeliveries(LocalDateTime before, int limit) {
		var deliveries = deliveryRepository.findRetryableDeliveries(before, PageRequest.of(0, limit));
		for (NewsletterDelivery delivery : deliveries) {
			delivery.setStatus("QUEUED");
			try {
				rabbitTemplate.convertAndSend(RabbitMQConfig.MAIL_EXCHANGE, RabbitMQConfig.MAIL_ROUTING_KEY,
						toMailMessage(delivery));
			} catch (Exception error) {
				recordFailure(delivery, error);
			}
		}
		return deliveries.size();
	}

	@Transactional(readOnly = true)
	public List<NewsletterDeliveryBatchResponse> getRecentBatches() {
		return batchRepository.findTop8ByOrderByStartedAtDesc().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public PageResult<NewsletterDeliveryResponse> getBatchDeliveries(Long batchId, Pageable pageable) {
		return PageResult.of(
				deliveryRepository.findByBatchIdOrderByCreatedAtDesc(batchId, pageable).map(this::toDeliveryResponse));
	}

	private void updateBatch(NewsletterDeliveryBatch batch) {
		long delivered = deliveryRepository.countByBatchAndStatus(batch, "DELIVERED");
		long failed = deliveryRepository.countByBatchAndStatus(batch, "ABANDONED");
		batch.setDeliveredCount(Math.toIntExact(delivered));
		batch.setFailedCount(Math.toIntExact(failed));
		if (delivered + failed >= batch.getQueuedCount()) {
			batch.setStatus(failed == 0 ? "DELIVERED" : "COMPLETED_WITH_FAILURES");
			batch.setCompletedAt(LocalDateTime.now());
		}
		batchRepository.save(batch);
	}

	private NewsletterDeliveryBatchResponse toResponse(NewsletterDeliveryBatch batch) {
		return new NewsletterDeliveryBatchResponse(batch.getId(), batch.getStatus(), batch.getRecipientCount(),
				batch.getQueuedCount(), batch.getDeliveredCount(), batch.getFailedCount(), batch.getStartedAt(),
				batch.getCompletedAt());
	}

	private NewsletterDeliveryResponse toDeliveryResponse(NewsletterDelivery delivery) {
		return new NewsletterDeliveryResponse(delivery.getId(), delivery.getSubscriber().getId(), delivery.getStatus(),
				delivery.getAttempts(), delivery.getLastError(), delivery.getDeliveredAt(), delivery.getCreatedAt());
	}

	private void recordFailure(NewsletterDelivery delivery, Exception error) {
		int attempts = currentAttempts(delivery) + 1;
		delivery.setAttempts(attempts);
		delivery.setStatus(attempts >= MAX_DELIVERY_ATTEMPTS ? "ABANDONED" : "FAILED");
		String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
		delivery.setLastError(message.substring(0, Math.min(1000, message.length())));
		deliveryRepository.save(delivery);
		updateBatch(delivery.getBatch());
	}

	private TemplateMailMessage toMailMessage(NewsletterDelivery delivery) {
		try {
			Map<String, Object> variables = objectMapper.readValue(delivery.getTemplateVariables(),
					new TypeReference<>() {
					});
			return TemplateMailMessage.builder().to(delivery.getRecipient()).subject(delivery.getSubject())
					.templateName(delivery.getTemplateName()).variables(variables)
					.type(TemplateMailMessage.MailType.TEMPLATE).newsletterDeliveryId(delivery.getId()).build();
		} catch (JsonProcessingException error) {
			throw new IllegalStateException("Unable to restore newsletter delivery payload", error);
		}
	}

	private String serializeVariables(Map<String, Object> variables) {
		try {
			return objectMapper.writeValueAsString(variables);
		} catch (JsonProcessingException error) {
			throw new IllegalStateException("Unable to persist newsletter delivery payload", error);
		}
	}

	private int currentAttempts(NewsletterDelivery delivery) {
		return delivery.getAttempts() == null ? 0 : delivery.getAttempts();
	}
}
