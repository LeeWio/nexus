package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import space.nebula.nexus.entity.LifeListItem;
import space.nebula.nexus.enums.LifeListStatus;
import space.nebula.nexus.enums.LifeListVisibility;

import java.util.Optional;

public interface LifeListItemRepository extends JpaRepository<LifeListItem, Long>, JpaSpecificationExecutor<LifeListItem> {

	Optional<LifeListItem> findByNumber(Integer number);

	boolean existsByNumber(Integer number);

	long countByCategoryId(Long categoryId);

	@Query("select coalesce(max(i.number), 0) from LifeListItem i")
	int findMaxNumber();

	@Query("select coalesce(max(i.sortOrder), 0) from LifeListItem i")
	int findMaxSortOrder();

	long countByVisibilityAndArchivedAtIsNull(LifeListVisibility visibility);

	long countByVisibilityAndStatusAndArchivedAtIsNull(LifeListVisibility visibility, LifeListStatus status);

	long countByArchivedAtIsNull();

	long countByStatusAndArchivedAtIsNull(LifeListStatus status);
}
