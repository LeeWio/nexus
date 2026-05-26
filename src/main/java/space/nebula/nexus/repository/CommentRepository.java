package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentStatus;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

	/**
	 * Find all comments for a specific post with specific status ordered by path.
	 * Uses EntityGraph to fetch user to avoid N+1.
	 */
	@EntityGraph(attributePaths = {"user"})
	List<Comment> findAllByPostIdAndStatusOrderByPathAsc(Long postId, CommentStatus status);

	@EntityGraph(attributePaths = {"user"})
	List<Comment> findAllByPostIsNullAndStatusOrderByPathAsc(CommentStatus status);

	/**
	 * Admin view: find all comments with pagination.
	 */
	@EntityGraph(attributePaths = {"user", "post"})
	Page<Comment> findAll(Pageable pageable);

	@EntityGraph(attributePaths = {"user", "post"})
	Page<Comment> findAllByStatus(CommentStatus status, Pageable pageable);

	long countByStatus(CommentStatus status);
}
