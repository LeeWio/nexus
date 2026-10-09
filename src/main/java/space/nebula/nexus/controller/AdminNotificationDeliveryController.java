package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.NotificationDeliveryStatus;
import space.nebula.nexus.payload.request.NotificationDeliveryRetryRequest;
import space.nebula.nexus.payload.response.*;
import space.nebula.nexus.service.NotificationDeliveryOperationsService;

@RestController
@RequestMapping("/api/v1/admin/notifications/deliveries")
@Tag(name = "Admin Notification Delivery", description = "Delivery outcomes, backlog and audited retries")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@RequiredArgsConstructor
public class AdminNotificationDeliveryController {
	private final NotificationDeliveryOperationsService operations;

	@GetMapping
	@Operation(summary = "List notification delivery outcomes", description = "Supports status and notification ID filters. Fixed newest-first pagination, maximum 100 rows. Recipient email and message bodies are omitted.")
	public ApiResponse<PageResult<NotificationDeliveryResponse>> list(
			@RequestParam(required = false) NotificationDeliveryStatus status,
			@RequestParam(required = false) @Positive Long notificationId,
			@PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(operations.list(status, notificationId, pageable));
	}
	@GetMapping("/overview")
	@Operation(summary = "Get notification delivery backlog", description = "Counts by state, due pending records and oldest pending creation time. Counts are an operational snapshot.")
	public ApiResponse<NotificationDeliveryOverviewResponse> overview() {
		return ApiResponse.success(operations.overview());
	}
	@GetMapping("/{id}")
	@Operation(summary = "Get one delivery outcome")
	public ApiResponse<NotificationDeliveryResponse> detail(@PathVariable @Positive Long id) {
		return ApiResponse.success(operations.detail(id));
	}
	@GetMapping("/{id}/retries")
	@Operation(summary = "List manual retry audit records")
	public ApiResponse<PageResult<NotificationDeliveryRetryResponse>> retries(@PathVariable @Positive Long id,
			@PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.success(operations.retries(id, pageable));
	}
	@PostMapping("/{id}/retry")
	@Operation(summary = "Request an audited delivery retry", description = "Only FAILED or ABANDONED deliveries are eligible. Keeps cumulative attempt numbers, grants at least five remaining attempts, and deduplicates requests by client UUID. Queued, sending or delivered records return conflict.")
	public ApiResponse<NotificationDeliveryResponse> retry(@PathVariable @Positive Long id,
			@Valid @RequestBody NotificationDeliveryRetryRequest request) {
		return ApiResponse.success(operations.retry(id, request));
	}
}
