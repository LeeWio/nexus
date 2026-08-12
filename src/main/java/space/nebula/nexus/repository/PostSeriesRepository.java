package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostSeries;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.projection.ColumnSummaryProjection;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostSeriesRepository extends JpaRepository<PostSeries, Long> {

	@EntityGraph(attributePaths = {"posts"})
	Optional<PostSeries> findBySlug(String slug);

	Optional<PostSeries> findFirstBySlug(String slug);

	@EntityGraph(attributePaths = {"posts"})
	List<PostSeries> findByIsPublishedTrueOrderByCreatedAtDesc();

	@EntityGraph(attributePaths = {"posts"})
	@Override
	java.util.List<PostSeries> findAll();

	boolean existsBySlug(String slug);

	@org.springframework.data.jpa.repository.Query("SELECT series.id AS id, series.name AS name, series.slug AS slug, "
			+ "series.description AS description, series.coverImage AS coverImage, COUNT(post.id) AS postsCount, "
			+ "series.createdAt AS createdAt FROM PostSeries series LEFT JOIN series.posts post ON post.status = :status "
			+ "WHERE series.isPublished = true GROUP BY series.id, series.name, series.slug, series.description, "
			+ "series.coverImage, series.createdAt ORDER BY series.createdAt DESC")
	List<ColumnSummaryProjection> findPublicColumnSummaries(PostStatus status);
}
