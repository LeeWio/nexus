package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import space.nebula.nexus.entity.NewsletterDelivery;
import space.nebula.nexus.entity.NewsletterDeliveryBatch;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsletterDeliveryRepository extends JpaRepository<NewsletterDelivery, Long> {
	long countByBatchAndStatus(NewsletterDeliveryBatch batch, String status);
	Page<NewsletterDelivery> findByBatchIdOrderByCreatedAtDesc(Long batchId, Pageable pageable);

	@Query("SELECT delivery FROM NewsletterDelivery delivery JOIN FETCH delivery.subscriber "
			+ "WHERE delivery.status = 'FAILED' AND delivery.updatedAt < :before ORDER BY delivery.updatedAt ASC")
	List<NewsletterDelivery> findRetryableDeliveries(LocalDateTime before, Pageable pageable);
}
