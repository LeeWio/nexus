package space.nebula.nexus.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import space.nebula.nexus.entity.MomentSubscriber;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MomentSubscriberRepository extends JpaRepository<MomentSubscriber, Long> {
	@Modifying
	@Query(value = """
			INSERT INTO blog_moment_subscriber (email, status, language, created_at)
			VALUES (:email, 'PENDING', :language, :now)
			ON DUPLICATE KEY UPDATE id = id
			""", nativeQuery = true)
	void ensureSubscriber(String email, String language, LocalDateTime now);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<MomentSubscriber> findByEmail(String email);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<MomentSubscriber> findByVerificationToken(String token);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<MomentSubscriber> findByUnsubscribeToken(String token);
}
