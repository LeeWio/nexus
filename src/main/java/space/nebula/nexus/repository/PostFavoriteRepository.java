package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostFavorite;
import space.nebula.nexus.entity.PostFavoriteId;
import space.nebula.nexus.enums.PostStatus;

import java.util.List;

/**
 * Provides read access to durable post favorites.
 */
@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, PostFavoriteId>
{
	/**
	 * Returns visible favorite posts for a user in reverse chronological order.
	 *
	 * @param userId user identifier
	 * @param status required post status
	 * @param pageable pagination settings
	 * @return favorite page
	 */
	@EntityGraph(attributePaths = { "post", "post.category", "post.author" })
	@Query("SELECT favorite FROM PostFavorite favorite WHERE favorite.user.id = :userId "
			+ "AND favorite.post.status = :status ORDER BY favorite.createdAt DESC")
	Page<PostFavorite> findVisibleFavorites(Long userId, PostStatus status, Pageable pageable);

	/**
	 * Returns the user's strongest favorite category signals.
	 *
	 * @param userId user identifier
	 * @param status required post status
	 * @param pageable result limit
	 * @return category identifiers ordered by signal strength and recency
	 */
	@Query("SELECT favorite.post.category.id FROM PostFavorite favorite WHERE favorite.user.id = :userId "
			+ "AND favorite.post.status = :status AND favorite.post.category IS NOT NULL "
			+ "GROUP BY favorite.post.category.id ORDER BY COUNT(favorite.id) DESC, MAX(favorite.createdAt) DESC")
	List<Long> findPreferredCategoryIds(Long userId, PostStatus status, Pageable pageable);
}
