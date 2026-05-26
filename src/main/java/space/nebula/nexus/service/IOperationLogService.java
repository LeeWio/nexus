package space.nebula.nexus.service;

import org.springframework.data.domain.Pageable;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.OperationLog;
import space.nebula.nexus.payload.response.PageResult;

public interface IOperationLogService
{

	/**
	 * Retrieves operation logs with filtering and pagination.
	 */
	ApiResponse<PageResult<OperationLog>> getOperationLogs(String username, String operation, Integer status,
			Pageable pageable);

	/**
	 * Deletes all operation logs.
	 */
	ApiResponse<Void> clearLogs();
}
