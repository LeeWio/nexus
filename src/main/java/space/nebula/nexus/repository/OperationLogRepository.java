package space.nebula.nexus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.OperationLog;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}
