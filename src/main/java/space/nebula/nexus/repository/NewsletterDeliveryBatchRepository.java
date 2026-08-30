package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import space.nebula.nexus.entity.NewsletterDeliveryBatch;

import java.util.List;

public interface NewsletterDeliveryBatchRepository extends JpaRepository<NewsletterDeliveryBatch, Long> {
	List<NewsletterDeliveryBatch> findTop8ByOrderByStartedAtDesc();
}
