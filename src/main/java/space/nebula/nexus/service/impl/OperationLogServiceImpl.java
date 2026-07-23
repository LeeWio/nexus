package space.nebula.nexus.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.OperationLog;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.repository.OperationLogRepository;
import space.nebula.nexus.service.IOperationLogService;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements IOperationLogService {

	private final OperationLogRepository operationLogRepository;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<PageResult<OperationLog>> getOperationLogs(String username, String operation, Integer status,
			Pageable pageable) {
		Specification<OperationLog> spec = (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (username != null && !username.isBlank()) {
				predicates.add(criteriaBuilder.like(root.get("username"), "%" + username + "%"));
			}
			if (operation != null && !operation.isBlank()) {
				predicates.add(criteriaBuilder.like(root.get("description"), "%" + operation + "%"));
			}
			if (status != null) {
				predicates.add(criteriaBuilder.equal(root.get("status"), status));
			}

			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};

		var page = operationLogRepository.findAll(spec, pageable);
		return ApiResponse.success(PageResult.of(page));
	}

	@Override
	@Transactional
	public ApiResponse<Void> clearLogs() {
		operationLogRepository.deleteAllInBatch();
		log.info("Admin cleared all operation logs.");
		return ApiResponse.success("Operation logs cleared successfully", null);
	}
}
