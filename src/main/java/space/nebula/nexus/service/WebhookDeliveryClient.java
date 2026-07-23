package space.nebula.nexus.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

/** Performs the outbound HTTP operation for a webhook delivery. */
@Component
@RequiredArgsConstructor
public class WebhookDeliveryClient {

	private final MeterRegistry meterRegistry;

	/** Result of one delivery attempt. */
	public record DeliveryResult(int statusCode, String responseBody, boolean success) {
	}

	/**
	 * Sends a signed webhook payload without following redirects.
	 *
	 * @param url
	 *            validated destination URL
	 * @param event
	 *            event name
	 * @param signature
	 *            HMAC signature
	 * @param jsonPayload
	 *            serialized payload
	 * @return delivery result
	 */
	public DeliveryResult post(String url, String event, String signature, String jsonPayload) {
		Timer.Sample sample = Timer.start(meterRegistry);
		String outcome = "error";
		try (HttpResponse response = HttpRequest.post(url).setFollowRedirects(false)
				.header("Content-Type", "application/json").header("X-Nexus-Signature", signature)
				.header("X-Nexus-Event", event).body(jsonPayload).timeout(10000).execute()) {
			outcome = response.isOk() ? "success" : "http_error";
			return new DeliveryResult(response.getStatus(), StrUtil.maxLength(response.body(), 16000), response.isOk());
		} finally {
			sample.stop(Timer.builder("nexus.webhook.delivery").description("Webhook delivery duration")
					.tag("event", event).tag("outcome", outcome).register(meterRegistry));
		}
	}
}
