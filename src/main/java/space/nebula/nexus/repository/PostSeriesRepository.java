package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostSeries;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostSeriesRepository extends JpaRepository<PostSeries, Long> {

	Optional<PostSeries> findBySlug(String slug);

	List<PostSeries> findByIsPublishedTrueOrderByCreatedAtDesc();

	boolean existsBySlug(String slug);
}
