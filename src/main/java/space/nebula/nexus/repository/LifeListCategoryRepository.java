package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import space.nebula.nexus.entity.LifeListCategory;

import java.util.List;
import java.util.Optional;

public interface LifeListCategoryRepository extends JpaRepository<LifeListCategory, Long> {

	Optional<LifeListCategory> findBySlug(String slug);

	boolean existsBySlug(String slug);

	boolean existsByName(String name);

	List<LifeListCategory> findAllByOrderBySortOrderAscNameAsc();

	@Query("select coalesce(max(c.sortOrder), 0) from LifeListCategory c")
	int findMaxSortOrder();
}
