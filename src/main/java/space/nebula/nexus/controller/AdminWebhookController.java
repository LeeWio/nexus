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

import java.util.List;

@Tag(name = "Admin Webhook Management", description = "Endpoints for managing system webhooks")
@RestController
@RequestMapping("/api/v1/admin/webhooks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminWebhookController {

	private final IWebhookService webhookService;

	@GetMapping
	@Operation(summary = "List all webhooks")
	public ApiResponse<List<WebhookResponse>> listWebhooks() {
		return webhookService.retrieveAllWebhooks();
	}

	@PostMapping
	@Operation(summary = "Create a webhook")
	public ApiResponse<WebhookResponse> createWebhook(@Valid @RequestBody WebhookRequest request) {
		return webhookService.createWebhook(request);
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a webhook")
	public ApiResponse<WebhookResponse> updateWebhook(@PathVariable Long id, @Valid @RequestBody WebhookRequest request) {
		return webhookService.updateWebhook(id, request);
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a webhook")
	public ApiResponse<Void> deleteWebhook(@PathVariable Long id) {
		return webhookService.deleteWebhook(id);
	}

	@PostMapping("/{id}/test")
	@Operation(summary = "Trigger a test ping to the webhook")
	public ApiResponse<Void> testWebhook(@PathVariable Long id) {
		return webhookService.triggerTestWebhook(id);
	}
}
