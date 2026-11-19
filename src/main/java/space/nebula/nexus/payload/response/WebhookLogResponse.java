package space.nebula.nexus.payload.response;

import space.nebula.nexus.entity.WebhookLog;

import java.time.LocalDateTime;

public record WebhookLogResponse(String deliveryId, String event, Integer responseCode, Boolean isSuccess,
		Integer attemptCount, LocalDateTime createdAt, boolean inProgress) {

	public static WebhookLogResponse from(WebhookLog log) {
		return new WebhookLogResponse(log.getDeliveryId(), log.getEvent(), log.getResponseCode(), log.getIsSuccess(),
				log.getAttemptCount(), log.getCreatedAt(), log.isInProgress(LocalDateTime.now()));
	}
}
