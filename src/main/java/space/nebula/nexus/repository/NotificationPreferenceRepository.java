package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.NotificationPreference;

import java.util.Optional;

/**
 * Stores the notification delivery controls selected by each user.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
	Optional<NotificationPreference> findByUserId(Long userId);

	Optional<NotificationPreference> findByUserIdAndIsDeletedFalse(Long userId);
}
