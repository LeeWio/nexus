package space.nebula.nexus.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import space.nebula.nexus.payload.response.WebhookLogResponse;
import space.nebula.nexus.security.OutboundUrlValidator;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDelivery {

	private final WebhookAttempts attempts;
	private final OutboundUrlValidator outboundUrlValidator;
	private final WebhookDeliveryClient client;

	public WebhookLogResponse send(WebhookAttempts.Attempt attempt) {
		WebhookDeliveryClient.DeliveryResult result = null;
		String error = null;
		try {
			outboundUrlValidator.validate(attempt.url());
			String signature = StrUtil.isBlank(attempt.secret())
					? ""
					: SecureUtil.hmac(HmacAlgorithm.HmacSHA256, attempt.secret().getBytes(StandardCharsets.UTF_8))
							.digestHex(attempt.payload());
			result = client.post(attempt.url(), attempt.event(), signature, attempt.payload(), attempt.deliveryId());
		} catch (RuntimeException failure) {
			// Exception messages can contain destination credentials or signed URL
			// parameters.
			error = failure.getClass().getSimpleName();
			log.warn("Webhook {} attempt {} failed: {}", attempt.deliveryId(), attempt.count(), error);
		}
		return attempts.complete(attempt, result, error);
	}
}
