package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.WebhookRequest;
import space.nebula.nexus.payload.response.WebhookResponse;
import space.nebula.nexus.service.IWebhookService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import io.swagger.v3.oas.annotations.Parameter;
import space.nebula.nexus.payload.response.PageResult;

import java.util.List;

@Tag(name = "Admin Webhook Management", description = "Endpoints for managing system webhooks")
@RestController
@RequestMapping("/api/v1/admin/webhooks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebhookController {

	private final IWebhookService webhookService;

	@GetMapping
	@Operation(summary = "List all webhooks", description = "Return every webhook configuration visible to administrators, including its target URL, subscribed events, and enabled state.")
	public ApiResponse<List<WebhookResponse>> listWebhooks() {
		return webhookService.retrieveAllWebhooks();
	}

	@PostMapping
	@Operation(summary = "Create a webhook", description = "Create a webhook delivery configuration. The returned object includes the generated identifier required for later updates, testing, and log queries.")
	public ApiResponse<WebhookResponse> createWebhook(@Valid @RequestBody WebhookRequest request) {
		return webhookService.createWebhook(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a webhook", description = "Replace the editable configuration for one webhook. Use the returned enabled state to keep management UI state synchronized.")
	public ApiResponse<WebhookResponse> updateWebhook(@Parameter(description = "Webhook ID") @PathVariable Long id,
			@Valid @RequestBody WebhookRequest request) {
		return webhookService.updateWebhook(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a webhook", description = "Permanently remove a webhook configuration. This cannot be undone.")
	public ApiResponse<Void> deleteWebhook(@Parameter(description = "Webhook ID") @PathVariable Long id) {
		return webhookService.deleteWebhook(id);
	}

	@PostMapping("/{id}/test")
	@Operation(summary = "Send a webhook test delivery", description = "Trigger a test delivery to validate one webhook target and its configuration. Inspect the delivery log for the resulting status.")
	public ApiResponse<Void> testWebhook(@Parameter(description = "Webhook ID") @PathVariable Long id) {
		return webhookService.triggerTestWebhook(id);
	}

	@GetMapping("/{id}/logs")
	@Operation(summary = "Get webhook logs", description = "Retrieve a paginated list of delivery logs for a specific webhook.")
	public ApiResponse<PageResult<space.nebula.nexus.entity.WebhookLog>> getWebhookLogs(
			@Parameter(description = "Webhook ID") @PathVariable Long id,
			@Parameter(description = "Pagination parameters") @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return webhookService.retrieveWebhookLogs(id, pageable);
	}

	@PostMapping("/logs/{deliveryId}/redeliver")
	@Operation(summary = "Redeliver a failed webhook delivery", description = "Retry one recorded failed delivery. The original delivery ID identifies the payload and destination to retry.")
	public ApiResponse<Void> redeliverWebhookLog(
			@Parameter(description = "Webhook Delivery ID") @PathVariable String deliveryId) {
		return webhookService.redeliverWebhookLog(deliveryId);
	}
}
