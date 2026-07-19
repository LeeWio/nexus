package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.PostCollection;
import space.nebula.nexus.payload.response.PostCollectionResponse;

import java.util.List;
import java.util.Optional;

/**
 * Manages user-owned post collections.
 */
@Repository
public interface PostCollectionRepository extends JpaRepository<PostCollection, Long>
{
	/** Returns a collection only when it belongs to the specified user. */
	Optional<PostCollection> findByIdAndUserIdAndIsDeletedFalse(Long id, Long userId);

	/** Checks collection-name uniqueness within one user's library. */
	boolean existsByUserIdAndNameIgnoreCaseAndIsDeletedFalse(Long userId, String name);

	/** Checks collection-name uniqueness while excluding the collection being edited. */
	boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndIsDeletedFalse(Long userId, String name, Long id);

	/** Counts active collections owned by a user. */
	long countByUserIdAndIsDeletedFalse(Long userId);

	/** Returns collection summaries with item counts in a single query. */
	@Query("SELECT new space.nebula.nexus.payload.response.PostCollectionResponse(collection.id, collection.name, "
			+ "collection.description, COUNT(item.id), collection.createdAt, collection.updatedAt) "
			+ "FROM PostCollection collection LEFT JOIN PostCollectionItem item "
			+ "ON item.collection = collection AND item.isDeleted = false "
			+ "WHERE collection.user.id = :userId AND collection.isDeleted = false "
			+ "GROUP BY collection.id, collection.name, collection.description, collection.createdAt, collection.updatedAt "
			+ "ORDER BY collection.createdAt DESC")
	List<PostCollectionResponse> findSummariesByUserId(Long userId);
}
