package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import space.nebula.nexus.entity.LifeListMemory;
import space.nebula.nexus.enums.LifeListVisibility;

import java.util.List;

public interface LifeListMemoryRepository extends JpaRepository<LifeListMemory, Long> {

	List<LifeListMemory> findByItemIdOrderByOccurredAtDescCreatedAtDesc(Long itemId);

	List<LifeListMemory> findByItemIdAndVisibilityOrderByOccurredAtDescCreatedAtDesc(Long itemId,
			LifeListVisibility visibility);

	long countByItemId(Long itemId);
}
