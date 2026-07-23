package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.entity.OperationLog;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.IOperationLogService;

/**
 * Controller for administrative operation log management.
 */
@Tag(name = "Admin Log Management", description = "Endpoints for auditing system operation logs")
@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

	private final IOperationLogService operationLogService;

	@GetMapping
	@Operation(summary = "Query operation logs", description = "Retrieve a paginated list of system operation logs with optional filters.")
	public ApiResponse<PageResult<OperationLog>> getLogs(
			@Parameter(description = "Filter by username") @RequestParam(required = false) String username,
			@Parameter(description = "Filter by operation name") @RequestParam(required = false) String operation,
			@Parameter(description = "Filter by status (0: Success, 1: Failure)") @RequestParam(required = false) Integer status,
			@Parameter(description = "Pagination and sorting") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return operationLogService.getOperationLogs(username, operation, status, pageable);
	}

	@DeleteMapping("/clear")
	@Operation(summary = "Clear all logs", description = "Permanently remove all historical operation logs from the database.")
	public ApiResponse<Void> clearLogs() {
		return operationLogService.clearLogs();
	}
}
