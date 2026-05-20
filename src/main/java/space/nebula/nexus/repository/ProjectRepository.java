package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByIsPublishedTrueOrderBySortOrderAscCreatedAtDesc();

	List<Project> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsPublishedTrue(String name,
			String description);
}
