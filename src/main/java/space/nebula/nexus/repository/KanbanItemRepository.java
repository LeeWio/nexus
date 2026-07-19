package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import space.nebula.nexus.entity.KanbanItem;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KanbanItemRepository extends JpaRepository<KanbanItem, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT i FROM KanbanItem i WHERE i.id = :id")
	Optional<KanbanItem> findByIdForUpdate(Long id);

	@Query("SELECT MAX(i.orderIndex) FROM KanbanItem i WHERE i.column.id = :columnId")
	Integer findMaxOrderIndexByColumnId(Long columnId);

	List<KanbanItem> findByColumnIdOrderByOrderIndexAscIdAsc(Long columnId);

	List<KanbanItem> findByReminderAtBeforeAndUpdatedAtBefore(LocalDateTime now, LocalDateTime threshold);

	// For simplicity in the reminder task, let's just find items where reminderAt
	// is due
	List<KanbanItem> findByReminderAtBefore(LocalDateTime now);
}
