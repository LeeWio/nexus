package space.nebula.nexus.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import space.nebula.nexus.entity.MomentEmailDelivery;
import space.nebula.nexus.entity.MomentEmailDelivery.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MomentEmailDeliveryRepository extends JpaRepository<MomentEmailDelivery, Long> {
	/** Captures the recipients and their current consent generations atomically. */
	@Modifying
	@Query(value = """
			INSERT INTO blog_moment_email_delivery
			    (subscriber_id, moment_id, generation, status, next_attempt_at, created_at)
			SELECT id, :momentId, generation, 'QUEUED', :publishedAt, :publishedAt
			FROM blog_moment_subscriber
			WHERE status = 'ACTIVE' AND verified_at <= :publishedAt AND is_deleted = FALSE
			ON DUPLICATE KEY UPDATE id = blog_moment_email_delivery.id
			""", nativeQuery = true)
	void enqueuePublication(Long momentId, LocalDateTime publishedAt);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM MomentEmailDelivery d WHERE d.status IN :statuses AND d.nextAttemptAt <= :now "
			+ "ORDER BY d.nextAttemptAt, d.id")
	List<MomentEmailDelivery> findDue(List<Status> statuses, LocalDateTime now, Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT d FROM MomentEmailDelivery d WHERE d.id = :id")
	Optional<MomentEmailDelivery> findForUpdate(Long id);
}
