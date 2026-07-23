package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.WebhookLog;

@Repository
public interface WebhookLogRepository extends JpaRepository<WebhookLog, Long> {
	Page<WebhookLog> findByWebhookId(Long webhookId, Pageable pageable);

	java.util.Optional<WebhookLog> findByDeliveryId(String deliveryId);
}
