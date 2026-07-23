package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.ReadingHistory;
import space.nebula.nexus.enums.PostStatus;

import java.util.Optional;
import java.util.List;

/**
 * Stores and retrieves resumable reading history.
 */
@Repository
public interface ReadingHistoryRepository extends JpaRepository<ReadingHistory, Long> {
	/** Returns the unique reading state for a user and post. */
	Optional<ReadingHistory> findByUserIdAndPostIdAndIsDeletedFalse(Long userId, Long postId);

	/** Returns visible reading history in most-recently-read order. */
	@EntityGraph(attributePaths = {"post", "post.category", "post.author"})
	@Query("SELECT history FROM ReadingHistory history WHERE history.user.id = :userId "
			+ "AND history.isDeleted = false AND history.post.status = :status ORDER BY history.lastReadAt DESC")
	Page<ReadingHistory> findVisibleHistory(Long userId, PostStatus status, Pageable pageable);

	/**
	 * Returns incomplete reading sessions for the personal overview.
	 *
	 * @param userId
	 *            user identifier
	 * @param status
	 *            required post status
	 * @param pageable
	 *            result limit
	 * @return incomplete reading sessions
	 */
	@EntityGraph(attributePaths = {"post", "post.category", "post.author"})
	@Query("SELECT history FROM ReadingHistory history WHERE history.user.id = :userId "
			+ "AND history.isDeleted = false AND history.progressPercent < 100 "
			+ "AND history.post.status = :status ORDER BY history.lastReadAt DESC")
	List<ReadingHistory> findContinuableHistory(Long userId, PostStatus status, Pageable pageable);

	/**
	 * Returns the user's strongest reading category signals.
	 *
	 * @param userId
	 *            user identifier
	 * @param status
	 *            required post status
	 * @param pageable
	 *            result limit
	 * @return category identifiers ordered by signal strength and recency
	 */
	@Query("SELECT history.post.category.id FROM ReadingHistory history WHERE history.user.id = :userId "
			+ "AND history.isDeleted = false AND history.post.status = :status AND history.post.category IS NOT NULL "
			+ "GROUP BY history.post.category.id ORDER BY COUNT(history.id) DESC, MAX(history.lastReadAt) DESC")
	List<Long> findPreferredCategoryIds(Long userId, PostStatus status, Pageable pageable);

	/** Physically removes one history entry owned by a user. */
	@Modifying
	@Query("DELETE FROM ReadingHistory history WHERE history.user.id = :userId AND history.post.id = :postId")
	int deleteOwnedEntry(Long userId, Long postId);

	/** Physically removes every history entry owned by a user. */
	@Modifying
	@Query("DELETE FROM ReadingHistory history WHERE history.user.id = :userId")
	int deleteAllOwnedEntries(Long userId);
}
