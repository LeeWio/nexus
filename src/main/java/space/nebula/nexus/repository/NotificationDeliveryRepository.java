package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import space.nebula.nexus.entity.NotificationDelivery;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
	@EntityGraph(attributePaths = "notification")
	@Query("SELECT d FROM NotificationDelivery d WHERE (:status IS NULL OR d.status = :status) AND (:notificationId IS NULL OR d.notification.id = :notificationId)")
	Page<NotificationDelivery> findOperationsPage(String status, Long notificationId, Pageable pageable);

	@Query("SELECT d.status AS status, COUNT(d) AS total FROM NotificationDelivery d GROUP BY d.status")
	List<StatusCount> countByStatus();
	interface StatusCount {
		String getStatus();
		long getTotal();
	}

	@Query("SELECT MIN(d.createdAt) FROM NotificationDelivery d WHERE d.status IN ('QUEUED', 'SENDING', 'FAILED')")
	LocalDateTime oldestPendingAt();
	@Query("SELECT COUNT(d) FROM NotificationDelivery d WHERE d.status IN ('QUEUED', 'SENDING', 'FAILED') AND d.nextAttemptAt <= :now")
	long countOverdue(LocalDateTime now);
	Optional<NotificationDelivery> findByNotificationIdAndChannel(Long notificationId, String channel);
	Optional<NotificationDelivery> findByDeduplicationKey(String deduplicationKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM NotificationDelivery d WHERE d.id = :id")
	Optional<NotificationDelivery> findForUpdate(Long id);

	@Modifying
	@Query(value = "INSERT INTO sys_notification_delivery "
			+ "(notification_id, channel, recipient, title, content, link, deduplication_key, status, attempts, max_attempts, enqueue_generation, next_attempt_at, created_at, updated_at, is_deleted) "
			+ "VALUES (:notificationId, 'EMAIL', :recipient, :title, :content, :link, :eventKey, 'QUEUED', 0, 5, 0, :nextAttemptAt, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), false) "
			+ "ON DUPLICATE KEY UPDATE id = id", nativeQuery = true)
	int insertEmailOnce(Long notificationId, String recipient, String title, String content, String link,
			String eventKey, LocalDateTime nextAttemptAt);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT delivery FROM NotificationDelivery delivery "
			+ "WHERE delivery.status IN :statuses AND delivery.nextAttemptAt <= :before "
			+ "ORDER BY delivery.nextAttemptAt ASC, delivery.id ASC")
	List<NotificationDelivery> findRetryableDeliveries(List<String> statuses, LocalDateTime before,
			org.springframework.data.domain.Pageable pageable);
}
