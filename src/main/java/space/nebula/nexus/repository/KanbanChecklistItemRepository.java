package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.KanbanChecklistItem;

import java.util.List;

@Repository
public interface KanbanChecklistItemRepository extends JpaRepository<KanbanChecklistItem, Long> {

	List<KanbanChecklistItem> findByTaskIdOrderByOrderIndexAscIdAsc(Long taskId);
}
