package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.WebhookRequest;
import space.nebula.nexus.payload.request.WebhookUpdateRequest;
import space.nebula.nexus.payload.response.WebhookLogResponse;
import space.nebula.nexus.payload.response.WebhookTestResponse;
import space.nebula.nexus.payload.response.WebhookResponse;

import java.util.List;

public interface IWebhookService {

	ApiResponse<List<WebhookResponse>> retrieveAllWebhooks();

	ApiResponse<WebhookResponse> createWebhook(WebhookRequest request);

	ApiResponse<WebhookResponse> updateWebhook(Long id, WebhookUpdateRequest request);

	ApiResponse<Void> deleteWebhook(Long id);

	ApiResponse<WebhookTestResponse> triggerTestWebhook(Long id);

	ApiResponse<WebhookLogResponse> redeliverWebhookLog(String deliveryId, int attemptCount);

	ApiResponse<space.nebula.nexus.payload.response.PageResult<WebhookLogResponse>> retrieveWebhookLogs(Long id,
			org.springframework.data.domain.Pageable pageable);
}
