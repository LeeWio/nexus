package space.nebula.nexus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import space.nebula.nexus.entity.NewsletterDelivery;
import space.nebula.nexus.entity.NewsletterDeliveryBatch;
import space.nebula.nexus.repository.NewsletterDeliveryBatchRepository;
import space.nebula.nexus.repository.NewsletterDeliveryRepository;

@ExtendWith(MockitoExtension.class)
class NewsletterDeliveryServiceTest {

	@Mock
	private NewsletterDeliveryBatchRepository batchRepository;

	@Mock
	private NewsletterDeliveryRepository deliveryRepository;

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private NewsletterDeliveryService newsletterDeliveryService;

	@Test
	void successfulRecipientMarksTheBatchDeliveredWhenItIsTheFinalOutcome() {
		NewsletterDeliveryBatch batch = batchWithQueuedRecipient();
		NewsletterDelivery delivery = deliveryFor(batch);
		when(deliveryRepository.findById(41L)).thenReturn(Optional.of(delivery));
		when(deliveryRepository.countByBatchAndStatus(batch, "DELIVERED")).thenReturn(1L);
		when(deliveryRepository.countByBatchAndStatus(batch, "ABANDONED")).thenReturn(0L);

		newsletterDeliveryService.markDelivered(41L);

		assertEquals("DELIVERED", delivery.getStatus());
		assertEquals(1, delivery.getAttempts());
		assertNotNull(delivery.getDeliveredAt());
		assertEquals("DELIVERED", batch.getStatus());
		assertEquals(1, batch.getDeliveredCount());
		assertNotNull(batch.getCompletedAt());
		verify(deliveryRepository).save(delivery);
		verify(batchRepository).save(batch);
	}

	@Test
	void failedRecipientStopsRetryingAtTheAttemptBudgetAndCompletesTheBatch() {
		NewsletterDeliveryBatch batch = batchWithQueuedRecipient();
		NewsletterDelivery delivery = deliveryFor(batch);
		delivery.setAttempts(4);
		when(deliveryRepository.findById(41L)).thenReturn(Optional.of(delivery));
		when(deliveryRepository.countByBatchAndStatus(batch, "DELIVERED")).thenReturn(0L);
		when(deliveryRepository.countByBatchAndStatus(batch, "ABANDONED")).thenReturn(1L);

		newsletterDeliveryService.markFailed(41L, new IllegalStateException("Mail gateway unavailable"));

		assertEquals("ABANDONED", delivery.getStatus());
		assertEquals(5, delivery.getAttempts());
		assertEquals("Mail gateway unavailable", delivery.getLastError());
		assertEquals("COMPLETED_WITH_FAILURES", batch.getStatus());
		assertEquals(1, batch.getFailedCount());
		assertNotNull(batch.getCompletedAt());
		verify(deliveryRepository).save(delivery);
		verify(batchRepository).save(batch);
	}

	private NewsletterDeliveryBatch batchWithQueuedRecipient() {
		NewsletterDeliveryBatch batch = new NewsletterDeliveryBatch();
		batch.setQueuedCount(1);
		batch.setStatus("PROCESSING");
		return batch;
	}

	private NewsletterDelivery deliveryFor(NewsletterDeliveryBatch batch) {
		NewsletterDelivery delivery = new NewsletterDelivery();
		delivery.setBatch(batch);
		delivery.setAttempts(0);
		delivery.setStatus("QUEUED");
		return delivery;
	}
}
