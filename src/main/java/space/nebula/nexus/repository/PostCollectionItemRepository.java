package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostCollectionItem;
import space.nebula.nexus.enums.PostStatus;

/**
 * Manages posts contained in personal collections.
 */
@Repository
public interface PostCollectionItemRepository extends JpaRepository<PostCollectionItem, Long>
{
	/** Checks whether a post is already present in a collection. */
	boolean existsByCollectionIdAndPostIdAndIsDeletedFalse(Long collectionId, Long postId);

	/** Counts active items in a collection. */
	long countByCollectionIdAndIsDeletedFalse(Long collectionId);

	/** Returns visible posts in reverse addition order. */
	@EntityGraph(attributePaths = { "post", "post.category", "post.author" })
	@Query("SELECT item FROM PostCollectionItem item WHERE item.collection.id = :collectionId "
			+ "AND item.isDeleted = false AND item.post.status = :status ORDER BY item.createdAt DESC")
	Page<PostCollectionItem> findVisibleItems(Long collectionId, PostStatus status, Pageable pageable);

	/** Physically removes one post from a collection. */
	@Modifying
	@Query("DELETE FROM PostCollectionItem item WHERE item.collection.id = :collectionId AND item.post.id = :postId")
	int deleteItem(Long collectionId, Long postId);

	/** Physically removes all items before deleting their collection. */
	@Modifying
	@Query("DELETE FROM PostCollectionItem item WHERE item.collection.id = :collectionId")
	int deleteAllItems(Long collectionId);
}
