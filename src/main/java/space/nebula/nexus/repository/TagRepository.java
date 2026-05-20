package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Tag;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
	Optional<Tag> findBySlug(String slug);
	Optional<Tag> findByName(String name);
	List<Tag> findByNameContainingIgnoreCase(String keyword);
}
