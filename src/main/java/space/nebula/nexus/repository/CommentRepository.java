package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentStatus;

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

	boolean existsByContentContaining(String keyword);
}
