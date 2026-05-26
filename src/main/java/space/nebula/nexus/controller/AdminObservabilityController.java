package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.service.IMetricsService;
import cn.hutool.core.lang.Dict;

@Tag(name = "Admin Observability", description = "Endpoints for monitoring system health and performance")
@RestController
@RequestMapping("/api/v1/admin/observability")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminObservabilityController
{

	private final IMetricsService metricsService;

	@GetMapping("/snapshot")
	@Operation(summary = "Get system performance snapshot", description = "Returns a real-time snapshot of JVM, HTTP, Cache, and MQ performance metrics.")
	public ApiResponse<Dict> getSystemSnapshot()
	{
		return ApiResponse.success(metricsService.getSystemPerformanceSnapshot());
	}
}
