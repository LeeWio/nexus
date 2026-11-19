package space.nebula.nexus.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.repository.projection.AssetReferenceText;
import space.nebula.nexus.entity.Project;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

	List<Project> findByIsPublishedTrueOrderBySortOrderAscCreatedAtDesc();

	@Query("""
			select p from Project p
			where p.isPublished = true and (locate(lower(:keyword), lower(p.name)) > 0
				or locate(lower(:keyword), lower(p.description)) > 0)
			order by p.sortOrder, p.name, p.id
			""")
	List<Project> searchPublishedProjects(@Param("keyword") String keyword, Pageable pageable);

	@org.springframework.data.jpa.repository.Query("""
			select new space.nebula.nexus.repository.projection.AssetReferenceText(p.id, p.name, p.coverImage)
			from Project p where p.coverImage is not null and (:term is null or locate(:term, p.coverImage) > 0)
			order by p.id
			""")
	List<AssetReferenceText> findAssetReferenceText(String term);

	boolean existsByCoverImageContaining(String keyword);
}
