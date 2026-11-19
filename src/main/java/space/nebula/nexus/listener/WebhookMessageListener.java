package space.nebula.nexus.listener;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.payload.request.WebhookMessage;
import space.nebula.nexus.service.WebhookAttempts;
import space.nebula.nexus.service.WebhookDelivery;

@Component
@RequiredArgsConstructor
public class WebhookMessageListener {

	private final WebhookAttempts attempts;
	private final WebhookDelivery delivery;

	@RabbitListener(queues = RabbitMQConfig.WEBHOOK_QUEUE)
	public void handleWebhookDispatch(WebhookMessage message) {
		String payload = JSONUtil.toJsonStr(message.getPayload());
		String deliveryId = StrUtil.blankToDefault(message.getDeliveryId(),
				SecureUtil.sha256(message.getWebhookId() + ":" + message.getEvent() + ":" + payload));
		var attempt = attempts.claimDispatch(message.getWebhookId(), deliveryId, message.getEvent(), payload);
		if (attempt.isPresent() && !Boolean.TRUE.equals(delivery.send(attempt.get()).isSuccess())) {
			throw new IllegalStateException("Webhook delivery failed: " + deliveryId);
		}
	}
}
