package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostSeries;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostSeriesRepository extends JpaRepository<PostSeries, Long> {

	@EntityGraph(attributePaths = {"posts"})
	Optional<PostSeries> findBySlug(String slug);

	@EntityGraph(attributePaths = {"posts"})
	List<PostSeries> findByIsPublishedTrueOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = {"posts"})
	@Override
	java.util.List<PostSeries> findAll();

	boolean existsBySlug(String slug);
}
