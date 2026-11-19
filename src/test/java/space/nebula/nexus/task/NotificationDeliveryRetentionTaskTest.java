package space.nebula.nexus.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.config.NotificationDeliveryRetentionProperties;
import space.nebula.nexus.repository.NotificationDeliveryRepository;
import space.nebula.nexus.repository.NotificationDeliveryRetryRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDeliveryRetentionTaskTest {
	@Mock
	private NotificationDeliveryRepository deliveries;
	@Mock
	private NotificationDeliveryRetryRepository retries;

	@Test
	void purgesTerminalDeliveriesAndRetryAuditsInBoundedBatches() {
		NotificationDeliveryRetentionProperties properties = new NotificationDeliveryRetentionProperties();
		properties.setBatchSize(2);
		NotificationDeliveryRetentionTask task = new NotificationDeliveryRetentionTask(deliveries, retries, properties);
		when(deliveries.findRetentionIds(any(), any(), any(Pageable.class))).thenReturn(List.of(1L, 2L), List.of());
		when(retries.findRetentionIds(any(), any(Pageable.class))).thenReturn(List.of(3L), List.of());

		task.purgeExpiredRecords();

		var order = inOrder(deliveries, retries);
		order.verify(deliveries).deleteAllByIdInBatch(List.of(1L, 2L));
		order.verify(retries).deleteAllByIdInBatch(List.of(3L));
	}
}
