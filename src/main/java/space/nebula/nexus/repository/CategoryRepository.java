package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Category;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
	Optional<Category> findBySlug(String slug);
	Optional<Category> findByName(String name);
	List<Category> findByNameContainingIgnoreCase(String keyword);

	@Query(value = "SELECT * FROM blog_category WHERE name = :name", nativeQuery = true)
	Optional<Category> findByNameIncludeDeleted(@Param("name") String name);

	@Query(value = "SELECT * FROM blog_category WHERE slug = :slug", nativeQuery = true)
	Optional<Category> findBySlugIncludeDeleted(@Param("slug") String slug);
}
