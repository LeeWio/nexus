package space.nebula.nexus.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.payload.response.NewsletterAudienceOverviewResponse;
import space.nebula.nexus.payload.response.NewsletterSubscriberResponse;
import space.nebula.nexus.payload.response.NewsletterDeliveryBatchResponse;
import space.nebula.nexus.payload.response.NewsletterDeliveryResponse;
import space.nebula.nexus.payload.response.PageResult;
import space.nebula.nexus.service.NewsletterDeliveryService;
import space.nebula.nexus.service.INewsletterService;

import java.util.List;

/** Administrative, token-free visibility into the newsletter audience. */
@Tag(name = "Admin Newsletter", description = "Audience metrics and subscriber lifecycle visibility")
@RestController
@RequestMapping("/api/v1/admin/newsletter")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNewsletterController {
	private final INewsletterService newsletterService;
	private final NewsletterDeliveryService newsletterDeliveryService;

	@GetMapping("/overview")
	@Operation(summary = "Get newsletter audience overview", description = "Returns aggregate subscriber lifecycle counts without exposing subscriber tokens.")
	public ApiResponse<NewsletterAudienceOverviewResponse> getOverview() {
		return newsletterService.getAudienceOverview();
	}

	@GetMapping("/subscribers")
	@Operation(summary = "List newsletter subscribers", description = "Returns a paginated, administrator-only subscriber list. Verification and unsubscribe tokens are never returned.")
	public ApiResponse<PageResult<NewsletterSubscriberResponse>> getSubscribers(
			@Parameter(description = "Optional lifecycle filter") @RequestParam(required = false) SubscriberStatus status,
			@Parameter(description = "Optional case-insensitive email search") @RequestParam(required = false) String query,
			@Parameter(description = "Zero-based pagination. Results default to newest subscription first.") @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return newsletterService.getSubscribers(status, query, pageable);
	}

	@GetMapping("/deliveries")
	@Operation(summary = "List recent newsletter deliveries", description = "Returns persisted broadcast outcomes. A delivered count only increments after the mail consumer completes successfully.")
	public ApiResponse<List<NewsletterDeliveryBatchResponse>> getRecentDeliveries() {
		return ApiResponse.success(newsletterDeliveryService.getRecentBatches());
	}

	@GetMapping("/deliveries/{batchId}")
	@Operation(summary = "List recipient delivery outcomes", description = "Returns token-free recipient delivery outcomes for one newsletter batch.")
	public ApiResponse<PageResult<NewsletterDeliveryResponse>> getBatchDeliveries(
			@Parameter(description = "Newsletter delivery batch ID") @PathVariable Long batchId,
			@PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.success(newsletterDeliveryService.getBatchDeliveries(batchId, pageable));
	}
}
