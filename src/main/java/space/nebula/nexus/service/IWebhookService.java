package space.nebula.nexus.service;

import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.payload.request.WebhookRequest;
import space.nebula.nexus.payload.response.WebhookResponse;

import java.util.List;

public interface IWebhookService {

	ApiResponse<List<WebhookResponse>> retrieveAllWebhooks();

	ApiResponse<WebhookResponse> createWebhook(WebhookRequest request);

	ApiResponse<WebhookResponse> updateWebhook(Long id, WebhookRequest request);

	ApiResponse<Void> deleteWebhook(Long id);

	ApiResponse<Void> triggerTestWebhook(Long id);
}
