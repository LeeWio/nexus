package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.CommentModerationLog;

@Repository
public interface CommentModerationLogRepository extends JpaRepository<CommentModerationLog, Long> {
}
