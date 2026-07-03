package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostStatus;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

	@EntityGraph(attributePaths = {"category", "author", "tags", "series", "parent"})
	Optional<Post> findBySlug(String slug);

	@EntityGraph(attributePaths = {"category", "author", "tags", "series", "parent"})
	Page<Post> findAllByStatus(PostStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"category", "author", "tags", "series", "parent"})
	Page<Post> findAllByCategoryIdAndStatus(Long categoryId, PostStatus status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"category", "author", "tags", "series", "parent"})
	Page<Post> findAll(Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"category", "author", "tags", "series", "parent"})
	Page<Post> findAll(@Nullable Specification<Post> spec, Pageable pageable);

	@Modifying
	@Query("update Post p set p.views = p.views + :count where p.id = :id")
	void incrementViews(Long id, Long count);

	@Modifying
	@Query("update Post p set p.status = 'PUBLISHED' where p.status = 'SCHEDULED' and p.publishedAt <= :now")
	int updateScheduledPosts(java.time.LocalDateTime now);

	@Modifying
	@Query("update Post p set p.likesCount = :likesCount, p.favoritesCount = :favoritesCount where p.id = :id")
	void updateSocialMetrics(Long id, Long likesCount, Long favoritesCount);

	@Query("SELECT p.content FROM Post p WHERE p.content IS NOT NULL")
	java.util.List<String> findAllContents();

	@Query("SELECT p.summary FROM Post p WHERE p.summary IS NOT NULL")
	java.util.List<String> findAllSummaries();

	@Query("SELECT SUM(p.views) FROM Post p")
	Long sumTotalViews();

	java.util.List<Post> findTop5ByTitleContainingIgnoreCaseAndStatus(String title, PostStatus status);

	java.util.List<Post> findByPathInOrderByPathAsc(java.util.Collection<String> paths);

	java.util.List<Post> findAllBySlugIn(java.util.List<String> slugs);

	// Navigation helpers
	@Query("SELECT p FROM Post p WHERE p.series.id = :seriesId AND p.status = 'PUBLISHED' AND p.seriesOrder < :currentOrder ORDER BY p.seriesOrder DESC, p.id DESC LIMIT 1")
	java.util.Optional<Post> findPreviousInSeries(Long seriesId, Integer currentOrder);

	@Query("SELECT p FROM Post p WHERE p.series.id = :seriesId AND p.status = 'PUBLISHED' AND p.seriesOrder > :currentOrder ORDER BY p.seriesOrder ASC, p.id ASC LIMIT 1")
	java.util.Optional<Post> findNextInSeries(Long seriesId, Integer currentOrder);

	boolean existsByCategoryId(Long categoryId);

	boolean existsByTagsId(Long tagId);

	boolean existsByContentContaining(String keyword);

	boolean existsBySummaryContaining(String keyword);
}
