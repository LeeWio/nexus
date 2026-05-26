package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.ApiResponse;
import space.nebula.nexus.common.annotation.LogOperation;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.mapper.WebhookMapper;
import space.nebula.nexus.payload.request.WebhookRequest;
import space.nebula.nexus.payload.response.WebhookResponse;
import space.nebula.nexus.repository.WebhookRepository;
import space.nebula.nexus.service.IWebhookService;
import space.nebula.nexus.listener.WebhookDispatcher;
import space.nebula.nexus.enums.WebhookEvent;
import cn.hutool.core.lang.Dict;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements IWebhookService {

	private final WebhookRepository webhookRepository;
	private final WebhookMapper webhookMapper;
	private final WebhookDispatcher webhookDispatcher;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<WebhookResponse>> retrieveAllWebhooks() {
		List<WebhookResponse> responses = webhookRepository.findAll().stream()
				.map(webhookMapper::toResponse)
				.collect(Collectors.toList());
		return ApiResponse.success(responses);
	}

	@Override
	@Transactional
	@LogOperation("Create Webhook")
	public ApiResponse<WebhookResponse> createWebhook(WebhookRequest request) {
		Webhook webhook = webhookMapper.toEntity(request);
		webhookRepository.save(webhook);
		log.info("Created new webhook: {}", webhook.getName());
		return ApiResponse.success("Webhook created successfully", webhookMapper.toResponse(webhook));
	}

	@Override
	@Transactional
	@LogOperation("Update Webhook")
	public ApiResponse<WebhookResponse> updateWebhook(Long id, WebhookRequest request) {
		Webhook webhook = webhookRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id));
		webhookMapper.updateEntity(webhook, request);
		webhookRepository.save(webhook);
		log.info("Updated webhook: {}", webhook.getName());
		return ApiResponse.success("Webhook updated successfully", webhookMapper.toResponse(webhook));
	}

	@Override
	@Transactional
	@LogOperation("Delete Webhook")
	public ApiResponse<Void> deleteWebhook(Long id) {
		Assert.isTrue(webhookRepository.existsById(id), () -> new ResourceNotFoundException("Webhook", "id", id));
		webhookRepository.deleteById(id);
		log.info("Deleted webhook ID: {}", id);
		return ApiResponse.success("Webhook deleted successfully", null);
	}

	@Override
	public ApiResponse<Void> triggerTestWebhook(Long id) {
		Webhook webhook = webhookRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Webhook", "id", id));
		
		Dict payload = Dict.create()
				.set("event", "ping")
				.set("timestamp", System.currentTimeMillis())
				.set("message", "Nexus Webhook Test Ping");
		
		webhookDispatcher.dispatchPayload(webhook, payload);
		
		return ApiResponse.success("Test webhook triggered", null);
	}
}
