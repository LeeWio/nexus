package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.repository.projection.AssetReferenceText;
import space.nebula.nexus.entity.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByIsPublishedTrueOrderBySortOrderAscCreatedAtDesc();

	List<Project> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseAndIsPublishedTrue(String name,
			String description);

	@org.springframework.data.jpa.repository.Query("""
			select new space.nebula.nexus.repository.projection.AssetReferenceText(p.id, p.name, p.coverImage)
			from Project p where p.coverImage is not null and (:term is null or locate(:term, p.coverImage) > 0)
			order by p.id
			""")
	List<AssetReferenceText> findAssetReferenceText(String term);

	boolean existsByCoverImageContaining(String keyword);
}
