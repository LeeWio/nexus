package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Tag;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
	Optional<Tag> findBySlug(String slug);
	Optional<Tag> findByName(String name);
	List<Tag> findByNameContainingIgnoreCase(String keyword);

	@Query(value = "SELECT * FROM blog_tag WHERE name = :name", nativeQuery = true)
	Optional<Tag> findByNameIncludeDeleted(@Param("name") String name);

	@Query(value = "SELECT * FROM blog_tag WHERE slug = :slug", nativeQuery = true)
	Optional<Tag> findBySlugIncludeDeleted(@Param("slug") String slug);
}
