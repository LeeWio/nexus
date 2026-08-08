package space.nebula.nexus.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
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
import space.nebula.nexus.security.OutboundUrlValidator;
import space.nebula.nexus.service.WebhookDeliveryClient;
import space.nebula.nexus.entity.WebhookLog;
import cn.hutool.core.lang.Dict;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements IWebhookService {

	private final WebhookRepository webhookRepository;
	private final space.nebula.nexus.repository.WebhookLogRepository webhookLogRepository;
	private final WebhookMapper webhookMapper;
	private final WebhookDispatcher webhookDispatcher;
	private final OutboundUrlValidator outboundUrlValidator;
	private final WebhookDeliveryClient deliveryClient;

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<WebhookResponse>> retrieveAllWebhooks() {
		List<WebhookResponse> responses = webhookRepository.findAll().stream().map(webhookMapper::toResponse)
				.collect(Collectors.toList());
		return ApiResponse.success(responses);
	}

	@Override
	@Transactional
	@LogOperation(value = "Create Webhook", logArgs = false)
	@org.springframework.cache.annotation.CacheEvict(value = space.nebula.nexus.common.constant.CacheConstants.SYS_CONFIG, key = "'active_webhooks'")
	public ApiResponse<WebhookResponse> createWebhook(WebhookRequest request) {
		outboundUrlValidator.validate(request.url());
		Webhook webhook = webhookMapper.toEntity(request);
		if (StrUtil.isBlank(webhook.getSecret())) {
			webhook.setSecret(IdUtil.fastSimpleUUID());
		}
		webhookRepository.save(webhook);
		log.info("Created new webhook: {}", webhook.getName());
		return ApiResponse.success("Webhook created successfully", webhookMapper.toResponse(webhook));
	}

	@Override
	@Transactional
	@LogOperation(value = "Update Webhook", logArgs = false)
	@org.springframework.cache.annotation.CacheEvict(value = space.nebula.nexus.common.constant.CacheConstants.SYS_CONFIG, key = "'active_webhooks'")
	public ApiResponse<WebhookResponse> updateWebhook(Long id, WebhookRequest request) {
		outboundUrlValidator.validate(request.url());
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
	@org.springframework.cache.annotation.CacheEvict(value = space.nebula.nexus.common.constant.CacheConstants.SYS_CONFIG, key = "'active_webhooks'")
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

		Dict payload = Dict.create().set("event", "ping").set("timestamp", System.currentTimeMillis()).set("message",
				"Nexus Webhook Test Ping");

		webhookDispatcher.dispatchPayload(webhook, payload);

		return ApiResponse.success("Test webhook task dispatched", null);
	}

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<space.nebula.nexus.payload.response.PageResult<space.nebula.nexus.entity.WebhookLog>> retrieveWebhookLogs(
			Long id, org.springframework.data.domain.Pageable pageable) {
		var logs = webhookLogRepository.findByWebhookId(id, pageable);
		return ApiResponse.success(space.nebula.nexus.payload.response.PageResult.of(logs));
	}

	@Override
	@Transactional
	@LogOperation("Redeliver Webhook Log")
	public ApiResponse<Void> redeliverWebhookLog(String deliveryId) {
		WebhookLog webhookLog = webhookLogRepository.findByDeliveryId(deliveryId)
				.orElseThrow(() -> new ResourceNotFoundException("WebhookLog", "deliveryId", deliveryId));
		Webhook webhook = webhookLog.getWebhook();
		Assert.isTrue(webhook != null && Boolean.TRUE.equals(webhook.getIsActive()) && !Boolean.TRUE.equals(webhook.getIsDeleted()),
				() -> new space.nebula.nexus.common.exception.BusinessException(
						space.nebula.nexus.common.constant.BusinessCode.BAD_REQUEST,
						"Webhook is inactive or has been deleted"));

		outboundUrlValidator.validate(webhook.getUrl());

		webhookLog.setAttemptCount(webhookLog.getAttemptCount() == null ? 1 : webhookLog.getAttemptCount() + 1);

		String signature = "";
		if (StrUtil.isNotBlank(webhook.getSecret())) {
			cn.hutool.crypto.digest.HMac mac = cn.hutool.crypto.SecureUtil.hmac(cn.hutool.crypto.digest.HmacAlgorithm.HmacSHA256, webhook.getSecret().getBytes());
			signature = mac.digestHex(webhookLog.getRequestPayload());
		}

		try {
			WebhookDeliveryClient.DeliveryResult response = deliveryClient.post(webhook.getUrl(),
					webhookLog.getEvent(), signature, webhookLog.getRequestPayload());
			webhookLog.setResponseCode(response.statusCode());
			webhookLog.setResponsePayload(response.responseBody());
			webhookLog.setIsSuccess(response.success());
			webhookLog.setErrorMessage(null);
			log.info("Successfully redelivered webhook {} to URL: {}", webhookLog.getEvent(), webhook.getUrl());
		} catch (Exception e) {
			log.error("Exception occurred while redelivering webhook {} to URL: {}", webhookLog.getEvent(), webhook.getUrl(), e);
			webhookLog.setIsSuccess(false);
			webhookLog.setErrorMessage(StrUtil.maxLength(e.getMessage(), 450));
		} finally {
			webhookLogRepository.save(webhookLog);
		}

		if (!Boolean.TRUE.equals(webhookLog.getIsSuccess())) {
			throw new space.nebula.nexus.common.exception.BusinessException(
					space.nebula.nexus.common.constant.BusinessCode.BAD_REQUEST,
					"Webhook redelivery failed: " + webhookLog.getErrorMessage());
		}

		return ApiResponse.success("Webhook redelivered successfully", null);
	}
}
