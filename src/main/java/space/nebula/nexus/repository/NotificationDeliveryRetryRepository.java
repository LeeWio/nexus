package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.entity.NotificationDeliveryRetry;

public interface NotificationDeliveryRetryRepository extends JpaRepository<NotificationDeliveryRetry, Long> {
	boolean existsByDeliveryIdAndRequestId(Long deliveryId, String requestId);
	Page<NotificationDeliveryRetry> findByDeliveryId(Long deliveryId, Pageable pageable);
}
