package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.Comment;
import space.nebula.nexus.enums.CommentStatus;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Find top-level comments (parent is null) for a specific post with specific status.
     * Uses EntityGraph to fetch user and children to avoid N+1.
     */
    @EntityGraph(attributePaths = {"user", "children", "children.user"})
    Page<Comment> findAllByPostIdAndParentIsNullAndStatus(Long postId, CommentStatus status, Pageable pageable);

    /**
     * Admin view: find all comments with pagination.
     */
    @EntityGraph(attributePaths = {"user", "post"})
    Page<Comment> findAll(Pageable pageable);
}
