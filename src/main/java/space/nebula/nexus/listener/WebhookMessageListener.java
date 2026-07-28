package space.nebula.nexus.listener;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.entity.WebhookLog;
import space.nebula.nexus.payload.request.WebhookMessage;
import space.nebula.nexus.repository.WebhookLogRepository;
import space.nebula.nexus.repository.WebhookRepository;
import space.nebula.nexus.security.OutboundUrlValidator;
import space.nebula.nexus.service.WebhookDeliveryClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookMessageListener {

	private final WebhookRepository webhookRepository;
	private final WebhookLogRepository webhookLogRepository;
	private final OutboundUrlValidator outboundUrlValidator;
	private final WebhookDeliveryClient deliveryClient;

	@RabbitListener(queues = RabbitMQConfig.WEBHOOK_QUEUE)
	public void handleWebhookDispatch(WebhookMessage message) {
		webhookRepository.findById(message.getWebhookId()).ifPresent(webhook -> {
			if (Boolean.FALSE.equals(webhook.getIsActive())) {
				return;
			}

			executeDispatch(webhook, message);
		});
	}

	private void executeDispatch(Webhook webhook, WebhookMessage message) {
		String deliveryId = message.getDeliveryId();
		if (StrUtil.isBlank(deliveryId)) {
			deliveryId = SecureUtil.sha256(
					message.getWebhookId() + ":" + message.getEvent() + ":" + JSONUtil.toJsonStr(message.getPayload()));
		}
		WebhookLog webhookLog = webhookLogRepository.findByDeliveryId(deliveryId).orElseGet(() -> {
			WebhookLog newLog = new WebhookLog();
			newLog.setAttemptCount(0);
			return newLog;
		});
		if (Boolean.TRUE.equals(webhookLog.getIsSuccess())) {
			log.debug("Skipping already delivered webhook message: {}", deliveryId);
			return;
		}

		webhookLog.setAttemptCount(webhookLog.getAttemptCount() == null ? 1 : webhookLog.getAttemptCount() + 1);

		String event = message.getEvent();
		Dict payload = message.getPayload();
		outboundUrlValidator.validate(webhook.getUrl());
		String jsonPayload = JSONUtil.toJsonStr(payload);
		String signature = "";

		if (StrUtil.isNotBlank(webhook.getSecret())) {
			HMac mac = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, webhook.getSecret().getBytes());
			signature = mac.digestHex(jsonPayload);
		}

		webhookLog.setDeliveryId(deliveryId);
		webhookLog.setWebhook(webhook);
		webhookLog.setEvent(event);
		webhookLog.setUrl(webhook.getUrl());
		webhookLog.setRequestPayload(jsonPayload);

		try {
			WebhookDeliveryClient.DeliveryResult response = deliveryClient.post(webhook.getUrl(), event, signature,
					jsonPayload);
			webhookLog.setResponseCode(response.statusCode());
			webhookLog.setResponsePayload(response.responseBody());
			webhookLog.setIsSuccess(response.success());

			if (response.success()) {
				log.debug("Successfully dispatched webhook {} to URL: {}", event, webhook.getUrl());
			} else {
				log.warn("Webhook dispatch failed for {} to URL: {}. HTTP Status: {}", event, webhook.getUrl(),
						response.statusCode());
			}
		} catch (Exception e) {
			log.error("Exception occurred while dispatching webhook {} to URL: {}", event, webhook.getUrl(), e);
			webhookLog.setIsSuccess(false);
			webhookLog.setErrorMessage(StrUtil.maxLength(e.getMessage(), 450));
		} finally {
			webhookLogRepository.save(webhookLog);
		}

		if (!Boolean.TRUE.equals(webhookLog.getIsSuccess())) {
			throw new IllegalStateException("Webhook delivery failed: " + deliveryId);
		}
	}
}
