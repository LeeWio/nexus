package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import space.nebula.nexus.entity.NotificationCategoryPreference;
import space.nebula.nexus.enums.NotificationCategory;
import java.util.List;
import java.util.Optional;

public interface NotificationCategoryPreferenceRepository extends JpaRepository<NotificationCategoryPreference, Long> {
	List<NotificationCategoryPreference> findByUserId(Long userId);
	Optional<NotificationCategoryPreference> findByUserIdAndCategory(Long userId, NotificationCategory category);
}
