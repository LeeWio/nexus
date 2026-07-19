package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import space.nebula.nexus.entity.KanbanColumn;

import java.util.List;

@Repository
public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {

	@Query("SELECT c FROM KanbanColumn c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.tags ORDER BY c.orderIndex ASC, i.orderIndex ASC")
	List<KanbanColumn> findAllWithItemsOrderByOrderIndexAsc();

	@Query("SELECT MAX(c.orderIndex) FROM KanbanColumn c")
	Integer findMaxOrderIndex();

	/** Locks selected columns in stable ID order for atomic board reordering. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM KanbanColumn c WHERE c.id IN :ids ORDER BY c.id")
	List<KanbanColumn> findAllByIdForUpdate(java.util.Collection<Long> ids);

	/** Locks every column in stable ID order for complete-board reordering. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM KanbanColumn c ORDER BY c.id")
	List<KanbanColumn> findAllForUpdate();
}
