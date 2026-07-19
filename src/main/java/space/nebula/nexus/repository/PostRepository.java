package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.enums.PostStatus;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

	/** Locks a post row while allocating dependent sequence-like values. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM Post p WHERE p.id = :id")
	Optional<Post> findByIdForUpdate(Long id);

	@EntityGraph(attributePaths = {"category", "author", "tags", "series", "parent"})
	Optional<Post> findBySlug(String slug);

	@EntityGraph(attributePaths = {"category", "author", "series", "parent"})
	Page<Post> findAllByStatus(PostStatus status, Pageable pageable);

	/** Returns highlighted published posts for the public discovery experience. */
	@EntityGraph(attributePaths = {"category", "author"})
	Page<Post> findAllByStatusAndIsFeaturedTrue(PostStatus status, Pageable pageable);

	/** Returns lightweight post pages for background scans without collection fetch joins. */
	@Query("SELECT p FROM Post p WHERE p.status = :status ORDER BY p.id")
	Page<Post> findScanPageByStatus(PostStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"category", "author", "series", "parent"})
	Page<Post> findAllByCategoryIdAndStatus(Long categoryId, PostStatus status, Pageable pageable);

	/**
	 * Returns a post with the relationships required by publication notifications.
	 *
	 * @param id post identifier
	 * @return post with category and author loaded
	 */
	@EntityGraph(attributePaths = {"category", "author"})
	@Query("SELECT post FROM Post post WHERE post.id = :id")
	Optional<Post> findPublicationNotificationPost(Long id);

	/**
	 * Returns published posts from categories explicitly followed by a user.
	 *
	 * @param userId user identifier
	 * @param status required post status
	 * @param pageable pagination settings
	 * @return followed-category post page
	 */
	@EntityGraph(attributePaths = {"category", "author"})
	@Query("SELECT post FROM Post post WHERE post.status = :status "
			+ "AND EXISTS (SELECT follow.id FROM CategoryFollow follow WHERE follow.user.id = :userId "
			+ "AND follow.category = post.category AND follow.isDeleted = false) "
			+ "ORDER BY post.publishedAt DESC, post.id DESC")
	Page<Post> findFollowedCategoryFeed(Long userId, PostStatus status, Pageable pageable);

	/**
	 * Finds unseen published posts within the user's preferred categories.
	 *
	 * @param userId user identifier
	 * @param categoryIds preferred category identifiers
	 * @param status required post status
	 * @param pageable result limit
	 * @return recommendation candidates ordered by editorial and engagement signals
	 */
	@EntityGraph(attributePaths = {"category", "author"})
	@Query("SELECT post FROM Post post WHERE post.status = :status AND post.category.id IN :categoryIds "
			+ "AND NOT EXISTS (SELECT history.id FROM ReadingHistory history WHERE history.user.id = :userId "
			+ "AND history.post = post AND history.isDeleted = false) "
			+ "AND NOT EXISTS (SELECT favorite.id FROM PostFavorite favorite WHERE favorite.user.id = :userId "
			+ "AND favorite.post = post) "
			+ "AND NOT EXISTS (SELECT hidden.id FROM HiddenRecommendation hidden WHERE hidden.user.id = :userId "
			+ "AND hidden.post = post AND hidden.isDeleted = false) "
			+ "ORDER BY post.isFeatured DESC, post.views DESC, post.likesCount DESC, post.publishedAt DESC, post.id DESC")
	List<Post> findPersonalizedRecommendations(Long userId, Collection<Long> categoryIds, PostStatus status,
			Pageable pageable);

	/**
	 * Finds popular unseen posts when personal preference signals are insufficient.
	 *
	 * @param userId user identifier
	 * @param status required post status
	 * @param pageable result limit
	 * @return popular unseen recommendation candidates
	 */
	@EntityGraph(attributePaths = {"category", "author"})
	@Query("SELECT post FROM Post post WHERE post.status = :status "
			+ "AND NOT EXISTS (SELECT history.id FROM ReadingHistory history WHERE history.user.id = :userId "
			+ "AND history.post = post AND history.isDeleted = false) "
			+ "AND NOT EXISTS (SELECT favorite.id FROM PostFavorite favorite WHERE favorite.user.id = :userId "
			+ "AND favorite.post = post) "
			+ "AND NOT EXISTS (SELECT hidden.id FROM HiddenRecommendation hidden WHERE hidden.user.id = :userId "
			+ "AND hidden.post = post AND hidden.isDeleted = false) "
			+ "ORDER BY post.isFeatured DESC, post.views DESC, post.likesCount DESC, post.publishedAt DESC, post.id DESC")
	List<Post> findPopularUnseenPosts(Long userId, PostStatus status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"category", "author", "series", "parent"})
	Page<Post> findAll(Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"category", "author", "series", "parent"})
	Page<Post> findAll(@Nullable Specification<Post> spec, Pageable pageable);

	@Modifying
	@Query("update Post p set p.views = p.views + :count where p.id = :id")
	void incrementViews(Long id, Long count);

	/**
	 * Returns identifiers for the next scheduled posts whose publication time has
	 * arrived. Selecting identifiers first preserves database-level pagination
	 * when the publication graph later fetches a collection.
	 *
	 * @param status scheduled status
	 * @param now publication cutoff
	 * @param pageable batch limit
	 * @return due post identifiers in deterministic publication order
	 */
	@Query("SELECT p.id FROM Post p WHERE p.status = :status AND p.scheduledAt <= :now "
			+ "ORDER BY p.scheduledAt ASC, p.id ASC")
	List<Long> findDueScheduledPostIds(PostStatus status, java.time.LocalDateTime now, Pageable pageable);

	/**
	 * Loads the complete graph required by publication side effects for a bounded
	 * identifier batch.
	 *
	 * @param status required status at lock acquisition time
	 * @param ids post identifiers
	 * @return scheduled posts with publication relationships initialized
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(attributePaths = {"category", "author", "tags", "series", "parent"})
	@Query("SELECT DISTINCT p FROM Post p WHERE p.status = :status AND p.id IN :ids")
	List<Post> findScheduledPublicationBatch(PostStatus status, Collection<Long> ids);

	@Modifying
	@Query("update Post p set p.likesCount = :likesCount, p.favoritesCount = :favoritesCount where p.id = :id")
	void updateSocialMetrics(Long id, Long likesCount, Long favoritesCount);

	@Modifying
	@Query("update Post p set p.likesCount = case when p.likesCount + :delta < 0 then 0 else p.likesCount + :delta end where p.id = :id")
	void incrementLikes(Long id, Long delta);

	@Modifying
	@Query("update Post p set p.favoritesCount = case when p.favoritesCount + :delta < 0 then 0 else p.favoritesCount + :delta end where p.id = :id")
	void incrementFavorites(Long id, Long delta);

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

	boolean existsByParentId(Long parentId);

	@Modifying(flushAutomatically = true)
	@Query(value = "UPDATE blog_post SET path = CONCAT(:newPrefix, SUBSTRING(path, CHAR_LENGTH(:oldPrefix) + 1)) "
			+ "WHERE id <> :postId AND path LIKE CONCAT(:oldPrefix, '%')", nativeQuery = true)
	int replaceDescendantPathPrefix(Long postId, String oldPrefix, String newPrefix);

	boolean existsByContentContaining(String keyword);

	boolean existsBySummaryContaining(String keyword);
}
