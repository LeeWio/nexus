package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.CategoryFollow;

import java.util.List;

/**
 * Stores explicit user category preferences.
 */
@Repository
public interface CategoryFollowRepository extends JpaRepository<CategoryFollow, Long>
{
	/**
	 * Returns followed categories in most-recently-followed order.
	 *
	 * @param userId user identifier
	 * @return category follow records
	 */
	@EntityGraph(attributePaths = "category")
	@Query("SELECT follow FROM CategoryFollow follow WHERE follow.user.id = :userId "
			+ "AND follow.isDeleted = false ORDER BY follow.createdAt DESC")
	List<CategoryFollow> findAllByUserId(Long userId);

	/**
	 * Returns followed category identifiers in most-recently-followed order.
	 *
	 * @param userId user identifier
	 * @return followed category identifiers
	 */
	@Query("SELECT follow.category.id FROM CategoryFollow follow WHERE follow.user.id = :userId "
			+ "AND follow.isDeleted = false ORDER BY follow.createdAt DESC")
	List<Long> findCategoryIdsByUserId(Long userId);

	/**
	 * Counts active category follows owned by a user.
	 *
	 * @param userId user identifier
	 * @return active follow count
	 */
	long countByUserIdAndIsDeletedFalse(Long userId);

	/**
	 * Checks whether a user already follows a category.
	 *
	 * @param userId user identifier
	 * @param categoryId category identifier
	 * @return {@code true} when the active follow exists
	 */
	boolean existsByUserIdAndCategoryIdAndIsDeletedFalse(Long userId, Long categoryId);

	/**
	 * Creates a follow unless it already exists.
	 *
	 * @param userId user identifier
	 * @param categoryId category identifier
	 * @return number of inserted rows
	 */
	@Modifying
	@Query(value = "INSERT IGNORE INTO blog_category_follow "
			+ "(user_id, category_id, created_at, updated_at, is_deleted) "
			+ "VALUES (:userId, :categoryId, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), false)", nativeQuery = true)
	int insertIgnore(Long userId, Long categoryId);

	/**
	 * Physically removes a category follow.
	 *
	 * @param userId user identifier
	 * @param categoryId category identifier
	 * @return number of removed rows
	 */
	@Modifying
	@Query("DELETE FROM CategoryFollow follow WHERE follow.user.id = :userId AND follow.category.id = :categoryId")
	int deleteOwnedFollow(Long userId, Long categoryId);
}
