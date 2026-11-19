package space.nebula.nexus.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.WebhookLog;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {
	Page<WebhookLog> findByWebhookId(Long webhookId, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select log from WebhookLog log where log.deliveryId = :deliveryId")
	java.util.Optional<WebhookLog> lockByDeliveryId(String deliveryId);
}
