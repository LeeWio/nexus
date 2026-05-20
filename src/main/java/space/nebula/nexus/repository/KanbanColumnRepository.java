package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.KanbanColumn;

import java.util.List;

@Repository
public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, Long> {

	@Query("SELECT c FROM KanbanColumn c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.tags ORDER BY c.orderIndex ASC, i.orderIndex ASC")
	List<KanbanColumn> findAllWithItemsOrderByOrderIndexAsc();

	@Query("SELECT MAX(c.orderIndex) FROM KanbanColumn c")
	Integer findMaxOrderIndex();
}
