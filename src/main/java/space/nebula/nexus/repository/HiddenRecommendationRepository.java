package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.HiddenRecommendation;

/**
 * Stores user feedback that suppresses individual recommendation candidates.
 */
@Repository
public interface HiddenRecommendationRepository extends JpaRepository<HiddenRecommendation, Long>
{
	/**
	 * Counts hidden posts owned by a user.
	 *
	 * @param userId user identifier
	 * @return hidden post count
	 */
	long countByUserIdAndIsDeletedFalse(Long userId);

	/**
	 * Hides a post unless the feedback already exists.
	 *
	 * @param userId user identifier
	 * @param postId post identifier
	 * @return number of inserted rows
	 */
	@Modifying
	@Query(value = "INSERT IGNORE INTO blog_recommendation_hidden "
			+ "(user_id, post_id, created_at, updated_at, is_deleted) "
			+ "VALUES (:userId, :postId, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3), false)", nativeQuery = true)
	int insertIgnore(Long userId, Long postId);

	/**
	 * Restores one hidden recommendation.
	 *
	 * @param userId user identifier
	 * @param postId post identifier
	 * @return number of removed rows
	 */
	@Modifying
	@Query("DELETE FROM HiddenRecommendation hidden WHERE hidden.user.id = :userId AND hidden.post.id = :postId")
	int deleteOwnedHiddenPost(Long userId, Long postId);

	/**
	 * Clears all hidden recommendation feedback for a user.
	 *
	 * @param userId user identifier
	 * @return number of removed rows
	 */
	@Modifying
	@Query("DELETE FROM HiddenRecommendation hidden WHERE hidden.user.id = :userId")
	int deleteAllOwnedHiddenPosts(Long userId);
}
