package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostSeries;
import space.nebula.nexus.enums.PostStatus;
import space.nebula.nexus.repository.projection.ColumnSummaryProjection;

import space.nebula.nexus.repository.projection.AssetReferenceText;

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

	@Query("SELECT series.id AS id, series.name AS name, series.slug AS slug, "
			+ "series.description AS description, series.coverImage AS coverImage, COUNT(post.id) AS postsCount, "
			+ "series.createdAt AS createdAt FROM PostSeries series LEFT JOIN series.posts post ON post.status = :status "
			+ "WHERE series.isPublished = true GROUP BY series.id, series.name, series.slug, series.description, "
			+ "series.coverImage, series.createdAt ORDER BY series.createdAt DESC")
	List<ColumnSummaryProjection> findPublicColumnSummaries(PostStatus status);

	@Query("""
			select new space.nebula.nexus.repository.projection.AssetReferenceText(r.id, r.name, r.coverImage)
			from PostSeries r where r.coverImage is not null and (:term is null or locate(:term, r.coverImage) > 0)
			order by r.id
			""")
	List<AssetReferenceText> findAssetReferenceText(String term);
}
