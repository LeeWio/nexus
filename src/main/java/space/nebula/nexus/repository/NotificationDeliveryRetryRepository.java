package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.entity.NotificationDeliveryRetry;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

public interface NotificationDeliveryRetryRepository extends JpaRepository<NotificationDeliveryRetry, Long> {
	boolean existsByDeliveryIdAndRequestId(Long deliveryId, String requestId);
	Page<NotificationDeliveryRetry> findByDeliveryId(Long deliveryId, Pageable pageable);

	@Modifying
	@Query("DELETE FROM NotificationDeliveryRetry retry WHERE retry.deliveryId IN :deliveryIds")
	int deleteByDeliveryIdIn(Collection<Long> deliveryIds);

	@Query("SELECT retry.id FROM NotificationDeliveryRetry retry WHERE retry.createdAt < :cutoff ORDER BY retry.id")
	List<Long> findRetentionIds(LocalDateTime cutoff, Pageable pageable);
}
