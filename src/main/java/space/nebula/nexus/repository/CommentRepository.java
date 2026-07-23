package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

	/**
	 * Find all comments for a specific post with specific status ordered by path.
	 * Uses EntityGraph to fetch user to avoid N+1.
	 */
	@EntityGraph(attributePaths = {"user"})
	List<Comment> findAllByPostIdAndStatusOrderByPathAsc(Long postId, CommentStatus status);

	@EntityGraph(attributePaths = {"user"})
	List<Comment> findAllByPostIsNullAndStatusOrderByPathAsc(CommentStatus status);

	@EntityGraph(attributePaths = {"user", "post"})
	Page<Comment> findAllByPostIdAndParentIsNullAndStatus(Long postId, CommentStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	Page<Comment> findAllByPostIsNullAndParentIsNullAndStatus(CommentStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post", "parent"})
	Page<Comment> findAllByParentIdAndStatus(Long parentId, CommentStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post", "parent"})
	Page<Comment> findAllByUserId(Long userId, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post", "parent"})
	Page<Comment> findAllByUserIdAndStatus(Long userId, CommentStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post", "parent"})
	java.util.Optional<Comment> findByUserIdAndClientRequestId(Long userId, String clientRequestId);

	@EntityGraph(attributePaths = {"user", "post"})
	List<Comment> findAllByPostIdAndParentIsNullAndStatusAndIdLessThanOrderByIdDesc(Long postId, CommentStatus status,
			Long cursor, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	List<Comment> findAllByPostIdAndParentIsNullAndStatusOrderByIdDesc(Long postId, CommentStatus status,
			Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	List<Comment> findAllByPostIsNullAndParentIsNullAndStatusAndIdLessThanOrderByIdDesc(CommentStatus status,
			Long cursor, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	List<Comment> findAllByPostIsNullAndParentIsNullAndStatusOrderByIdDesc(CommentStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post", "parent"})
	List<Comment> findAllByParentIdAndStatusAndIdGreaterThanOrderByIdAsc(Long parentId, CommentStatus status,
			Long cursor, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post", "parent"})
	List<Comment> findAllByParentIdAndStatusOrderByIdAsc(Long parentId, CommentStatus status, Pageable pageable);

	long countByPostIdAndParentIsNullAndStatusAndIdGreaterThan(Long postId, CommentStatus status, Long afterId);

	long countByPostIsNullAndParentIsNullAndStatusAndIdGreaterThan(CommentStatus status, Long afterId);

	@EntityGraph(attributePaths = {"user", "post"})
	@Query("select c from Comment c where c.post.id = :postId and c.parent is null and c.status = :status and c.id > :afterId order by c.id asc")
	List<Comment> findNewRootCommentsByPost(Long postId, CommentStatus status, Long afterId, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	@Query("select c from Comment c where c.post is null and c.parent is null and c.status = :status and c.id > :afterId order by c.id asc")
	List<Comment> findNewGuestbookRootComments(CommentStatus status, Long afterId, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	@Query("select c from Comment c where c.post.id = :postId and c.parent is null and c.status = :status order by c.pinned desc, c.featured desc, c.likesCount desc, c.createdAt desc, c.id desc")
	Page<Comment> findHotRootCommentsByPost(Long postId, CommentStatus status, Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	@Query("select c from Comment c where c.post is null and c.parent is null and c.status = :status order by c.pinned desc, c.featured desc, c.likesCount desc, c.createdAt desc, c.id desc")
	Page<Comment> findHotGuestbookRootComments(CommentStatus status, Pageable pageable);

	@Override
	@EntityGraph(attributePaths = {"user", "post"})
	Page<Comment> findAll(@Nullable Specification<Comment> spec, Pageable pageable);

	/**
	 * Admin view: find all comments with pagination.
	 */
	@Override
	@EntityGraph(attributePaths = {"user", "post"})
	Page<Comment> findAll(Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	Page<Comment> findAllByStatus(CommentStatus status, Pageable pageable);

	long countByStatus(CommentStatus status);

	long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

	@org.springframework.data.jpa.repository.Query("SELECT c.content FROM Comment c WHERE c.content IS NOT NULL")
	java.util.List<String> findAllContents();

	boolean existsByContentContaining(String keyword);

	boolean existsByParentId(Long parentId);

	boolean existsByParentIdAndStatus(Long parentId, CommentStatus status);

	long countByParentIdAndStatus(Long parentId, CommentStatus status);

	@Query("select c.parent.id as parentId, count(c.id) as replyCount from Comment c where c.parent.id in :parentIds and c.status = :status group by c.parent.id")
	List<CommentReplyCountView> countRepliesByParentIds(Collection<Long> parentIds, CommentStatus status);

	@Query(value = "select comment_id from blog_comment_like where user_id = :userId and comment_id in (:commentIds)", nativeQuery = true)
	List<Long> findLikedCommentIds(Long userId, Collection<Long> commentIds);

	@Modifying
	@Query("update Comment c set c.likesCount = case when c.likesCount + :delta < 0 then 0 else c.likesCount + :delta end where c.id = :id")
	void incrementLikes(Long id, Long delta);

	@Modifying
	@Query("update Comment c set c.reportsCount = c.reportsCount + :delta where c.id = :id")
	void incrementReports(Long id, Long delta);

	interface CommentReplyCountView
	{
		Long getParentId();

		Long getReplyCount();
	}
}
