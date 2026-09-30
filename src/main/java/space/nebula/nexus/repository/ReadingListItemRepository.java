package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.ReadingListItem;
import space.nebula.nexus.enums.PostStatus;

/**
 * Stores and retrieves the current user's later-reading queue.
 */
@Repository
public interface ReadingListItemRepository extends JpaRepository<ReadingListItem, Long> {
	/** Returns visible reading-list entries in reverse addition order. */
	@EntityGraph(attributePaths = {"post", "post.category", "post.author"})
	@Query("SELECT item FROM ReadingListItem item WHERE item.user.id = :userId "
			+ "AND item.isDeleted = false AND item.post.status = :status ORDER BY item.createdAt DESC")
	Page<ReadingListItem> findVisibleItems(Long userId, PostStatus status, Pageable pageable);

	/** Checks whether a user already queued a post. */
	boolean existsByUserIdAndPostIdAndIsDeletedFalse(Long userId, Long postId);

	/** Inserts a queue entry without failing repeated requests. */
	@Modifying
	@Query(value = "INSERT IGNORE INTO blog_reading_list_item (user_id, post_id, created_at, is_deleted) "
			+ "VALUES (:userId, :postId, CURRENT_TIMESTAMP(3), FALSE)", nativeQuery = true)
	int insertIgnore(Long userId, Long postId);

	/** Physically removes one queue entry owned by a user. */
	@Modifying
	@Query("DELETE FROM ReadingListItem item WHERE item.user.id = :userId AND item.post.id = :postId")
	int deleteOwnedItem(Long userId, Long postId);
}
