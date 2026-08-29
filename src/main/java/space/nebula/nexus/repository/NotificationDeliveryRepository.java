package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import space.nebula.nexus.entity.NotificationDelivery;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
	Optional<NotificationDelivery> findByNotificationIdAndChannel(Long notificationId, String channel);

	@Query("SELECT delivery FROM NotificationDelivery delivery JOIN FETCH delivery.notification "
			+ "WHERE delivery.status IN :statuses AND delivery.updatedAt < :before "
			+ "ORDER BY delivery.updatedAt ASC")
	List<NotificationDelivery> findRetryableDeliveries(List<String> statuses, LocalDateTime before,
			org.springframework.data.domain.Pageable pageable);
}
