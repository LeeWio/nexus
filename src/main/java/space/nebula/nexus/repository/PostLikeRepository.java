package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostLike;
import space.nebula.nexus.entity.PostLikeId;
import space.nebula.nexus.enums.PostStatus;

import java.util.List;

/**
 * Provides read access to durable post likes without coupling library queries
 * to interaction write SQL.
 */
@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
	/** Returns published posts liked by a user in reverse chronological order. */
	@EntityGraph(attributePaths = {"post", "post.category", "post.author"})
	@Query("SELECT postLike FROM PostLike postLike WHERE postLike.user.id = :userId "
			+ "AND postLike.post.status = :status ORDER BY postLike.createdAt DESC")
	Page<PostLike> findVisibleLikes(Long userId, PostStatus status, Pageable pageable);

	/** Returns strongest category signals inferred from a user's liked posts. */
	@Query("SELECT postLike.post.category.id FROM PostLike postLike WHERE postLike.user.id = :userId "
			+ "AND postLike.post.status = :status AND postLike.post.category IS NOT NULL "
			+ "GROUP BY postLike.post.category.id ORDER BY COUNT(postLike.post.id) DESC, "
			+ "MAX(postLike.createdAt) DESC")
	List<Long> findPreferredCategoryIds(Long userId, PostStatus status, Pageable pageable);
}
