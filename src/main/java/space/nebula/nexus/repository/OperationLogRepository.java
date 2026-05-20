package space.nebula.nexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import space.nebula.nexus.entity.OperationLog;

@Repository
public interface OperationLogRepository
		extends
			JpaRepository<OperationLog, Long>,
			JpaSpecificationExecutor<OperationLog> {

	Page<OperationLog> findAllByUsernameContaining(String username, Pageable pageable);

	Page<OperationLog> findAllByDescriptionContaining(String description, Pageable pageable);

	Page<OperationLog> findAllByStatus(Integer status, Pageable pageable);
}
