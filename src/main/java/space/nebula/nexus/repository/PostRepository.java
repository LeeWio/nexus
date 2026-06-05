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

	@EntityGraph(attributePaths = {"category", "author", "tags"})
	Optional<Post> findBySlug(String slug);

	@EntityGraph(attributePaths = {"category", "author", "tags"})
	Page<Post> findAllByStatus(PostStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"category", "author", "tags"})
	Page<Post> findAllByCategoryIdAndStatus(Long categoryId, PostStatus status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"category", "author", "tags"})
	Page<Post> findAll(Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"category", "author", "tags"})
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

	@Query("SELECT SUM(p.views) FROM Post p")
	Long sumTotalViews();

	java.util.List<Post> findTop5ByTitleContainingIgnoreCaseAndStatus(String title, PostStatus status);
}
