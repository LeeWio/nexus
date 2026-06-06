package space.nebula.nexus.listener;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.entity.WebhookLog;
import space.nebula.nexus.payload.request.WebhookMessage;
import space.nebula.nexus.repository.WebhookLogRepository;
import space.nebula.nexus.repository.WebhookRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookMessageListener {

    private final WebhookRepository webhookRepository;
    private final WebhookLogRepository webhookLogRepository;

    @RabbitListener(queues = RabbitMQConfig.WEBHOOK_QUEUE)
    @Transactional
    public void handleWebhookDispatch(WebhookMessage message) {
        webhookRepository.findById(message.getWebhookId()).ifPresent(webhook -> {
            if (Boolean.FALSE.equals(webhook.getIsActive())) {
                return;
            }
            
            executeDispatch(webhook, message.getEvent(), message.getPayload());
        });
    }

    private void executeDispatch(Webhook webhook, String event, Dict payload) {
        String jsonPayload = JSONUtil.toJsonStr(payload);
        String signature = "";
        
        if (StrUtil.isNotBlank(webhook.getSecret())) {
            HMac mac = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, webhook.getSecret().getBytes());
            signature = mac.digestHex(jsonPayload);
        }

        WebhookLog webhookLog = new WebhookLog();
        webhookLog.setWebhook(webhook);
        webhookLog.setEvent(event);
        webhookLog.setUrl(webhook.getUrl());
        webhookLog.setRequestPayload(jsonPayload);

        try (HttpResponse response = HttpRequest.post(webhook.getUrl())
                .header("Content-Type", "application/json")
                .header("X-Nexus-Signature", signature)
                .header("X-Nexus-Event", event)
                .body(jsonPayload)
                .timeout(10000) // 10 seconds timeout for reliability
                .execute()) {

            webhookLog.setResponseCode(response.getStatus());
            webhookLog.setResponsePayload(response.body());
            webhookLog.setIsSuccess(response.isOk());
            
            if (response.isOk()) {
                log.debug("Successfully dispatched webhook {} to URL: {}", event, webhook.getUrl());
            } else {
                log.warn("Webhook dispatch failed for {} to URL: {}. HTTP Status: {}", event, webhook.getUrl(), response.getStatus());
            }
        } catch (Exception e) {
            log.error("Exception occurred while dispatching webhook {} to URL: {}", event, webhook.getUrl(), e);
            webhookLog.setIsSuccess(false);
            webhookLog.setErrorMessage(StrUtil.maxLength(e.getMessage(), 450));
        } finally {
            webhookLogRepository.save(webhookLog);
        }
    }
}
