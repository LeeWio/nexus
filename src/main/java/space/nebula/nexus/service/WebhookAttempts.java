package space.nebula.nexus.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.common.constant.BusinessCode;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.common.exception.ResourceNotFoundException;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.entity.WebhookLog;
import space.nebula.nexus.payload.response.WebhookLogResponse;
import space.nebula.nexus.repository.WebhookLogRepository;
import space.nebula.nexus.repository.WebhookRepository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Serializes claims and results in short transactions around the outbound
 * request.
 */
@Service
@RequiredArgsConstructor
public class WebhookAttempts {

	private final WebhookLogRepository logs;
	private final WebhookRepository webhooks;

	public record Attempt(String deliveryId, int count, String url, String secret, String event, String payload) {
	}

	@Transactional
	public Attempt claimRetry(String deliveryId, int expectedCount) {
		WebhookLog log = logs.lockByDeliveryId(deliveryId)
				.orElseThrow(() -> new ResourceNotFoundException("WebhookLog", "deliveryId", deliveryId));
		Webhook webhook = findActiveWebhook(log.getWebhook().getId()).orElseThrow(
				() -> new BusinessException(BusinessCode.BAD_REQUEST, "Enable the webhook before retrying"));
		if (!canClaim(log) || !Objects.equals(log.getAttemptCount(), expectedCount)) {
			throw new OptimisticLockingFailureException("Webhook delivery changed: " + deliveryId);
		}
		return claim(log, webhook);
	}

	@Transactional
	public Optional<Attempt> claimDispatch(Long webhookId, String deliveryId, String event, String payload) {
		var active = findActiveWebhook(webhookId);
		if (active.isEmpty()) {
			return Optional.empty();
		}
		Webhook webhook = active.get();
		WebhookLog log = logs.lockByDeliveryId(deliveryId).orElseGet(() -> {
			WebhookLog fresh = new WebhookLog();
			fresh.setWebhook(webhook);
			fresh.setDeliveryId(deliveryId);
			fresh.setEvent(event);
			fresh.setRequestPayload(payload);
			fresh.setAttemptCount(0);
			return fresh;
		});
		if (!canClaim(log)) {
			return Optional.empty();
		}
		return Optional.of(claim(log, webhook));
	}

	@Transactional
	public WebhookLogResponse complete(Attempt attempt, WebhookDeliveryClient.DeliveryResult result, String error) {
		WebhookLog log = logs.lockByDeliveryId(attempt.deliveryId())
				.orElseThrow(() -> new ResourceNotFoundException("WebhookLog", "deliveryId", attempt.deliveryId()));
		if (!Objects.equals(log.getAttemptCount(), attempt.count()) || log.getIsSuccess() != null) {
			throw new OptimisticLockingFailureException("Webhook delivery changed: " + attempt.deliveryId());
		}
		log.setIsSuccess(result != null && result.success());
		log.setResponseCode(result == null ? null : result.statusCode());
		log.setResponsePayload(result == null ? null : result.responseBody());
		log.setErrorMessage(error);
		return WebhookLogResponse.from(log);
	}

	private Optional<Webhook> findActiveWebhook(Long webhookId) {
		return webhooks.findById(webhookId).filter(webhook -> Boolean.TRUE.equals(webhook.getIsActive()));
	}

	private static boolean canClaim(WebhookLog log) {
		return !Boolean.TRUE.equals(log.getIsSuccess()) && !log.isInProgress(LocalDateTime.now());
	}

	private Attempt claim(WebhookLog log, Webhook webhook) {
		int count = log.getAttemptCount() + 1;
		log.setAttemptCount(count);
		log.setUrl(webhook.getUrl());
		log.setIsSuccess(null);
		log.setResponseCode(null);
		log.setResponsePayload(null);
		log.setErrorMessage(null);
		logs.save(log);
		return new Attempt(log.getDeliveryId(), count, webhook.getUrl(), webhook.getSecret(), log.getEvent(),
				log.getRequestPayload());
	}
}
