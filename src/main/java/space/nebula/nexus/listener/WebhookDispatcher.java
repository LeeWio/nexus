package space.nebula.nexus.listener;

import cn.hutool.core.collection.CollUtil;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.common.event.PostChangedEvent;
import space.nebula.nexus.common.event.PostChangeType;
import space.nebula.nexus.common.event.PostDeletedEvent;
import space.nebula.nexus.entity.Webhook;
import space.nebula.nexus.enums.WebhookEvent;
import space.nebula.nexus.repository.WebhookRepository;
import space.nebula.nexus.mapper.WebhookMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookDispatcher
{

	private final WebhookRepository webhookRepository;
	private final WebhookMapper webhookMapper;
	private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;
	private final space.nebula.nexus.config.RabbitMQConfig rabbitMQConfig;

	@Async("asyncExecutor")
	@org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
	public void handlePostChangedEvent(PostChangedEvent event)
	{
		if (event.getChangeType() == PostChangeType.PUBLISHED)
		{
			dispatchToSubscribers(WebhookEvent.POST_PUBLISHED,
					Dict.create().set("postId", event.getPost().getId()).set("title", event.getPost().getTitle())
							.set("slug", event.getPost().getSlug()).set("status", event.getPost().getStatus().name()));
		}
	}

	@Async("asyncExecutor")
	@org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
	public void handlePostDeletedEvent(PostDeletedEvent event)
	{
		dispatchToSubscribers(WebhookEvent.POST_DELETED,
				Dict.create().set("postId", event.getPostId()).set("slug", event.getSlug()));
	}

	@Async("asyncExecutor")
	@org.springframework.transaction.event.TransactionalEventListener(phase = org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
	public void handleCommentSubmittedEvent(CommentSubmittedEvent event)
	{
		dispatchToSubscribers(WebhookEvent.COMMENT_SUBMITTED, Dict.create().set("commentId", event.getComment().getId())
				.set("author", event.getComment().getUser().getUsername())
				.set("content", event.getComment().getContent()).set("status", event.getComment().getStatus().name()));
	}

	private void dispatchToSubscribers(WebhookEvent eventType, Dict payload)
	{
		List<Webhook> activeWebhooks = getActiveWebhooks();

		for (Webhook webhook : activeWebhooks)
		{
			List<WebhookEvent> subscribedEvents = webhookMapper.stringToEvents(webhook.getEvents());
			if (CollUtil.contains(subscribedEvents, eventType))
			{
				payload.set("event", eventType.name());
				payload.set("timestamp", System.currentTimeMillis());
				
				var message = space.nebula.nexus.payload.request.WebhookMessage.builder()
						.webhookId(webhook.getId())
						.deliveryId(cn.hutool.core.util.IdUtil.fastSimpleUUID())
						.event(eventType.name())
						.payload(payload)
						.build();
				
				rabbitTemplate.convertAndSend(space.nebula.nexus.config.RabbitMQConfig.WEBHOOK_EXCHANGE, 
						space.nebula.nexus.config.RabbitMQConfig.WEBHOOK_ROUTING_KEY, message);
			}
		}
	}

	@org.springframework.cache.annotation.Cacheable(value = space.nebula.nexus.common.constant.CacheConstants.SYS_CONFIG, key = "'active_webhooks'")
	public List<Webhook> getActiveWebhooks()
	{
		return webhookRepository.findAllByIsActiveTrue();
	}

	/**
	 * Triggered manually for testing.
	 */
	public void dispatchPayload(Webhook webhook, Dict payload)
	{
		var message = space.nebula.nexus.payload.request.WebhookMessage.builder()
				.webhookId(webhook.getId())
				.deliveryId(cn.hutool.core.util.IdUtil.fastSimpleUUID())
				.event(payload.getStr("event"))
				.payload(payload)
				.build();
		
		rabbitTemplate.convertAndSend(space.nebula.nexus.config.RabbitMQConfig.WEBHOOK_EXCHANGE, 
				space.nebula.nexus.config.RabbitMQConfig.WEBHOOK_ROUTING_KEY, message);
	}
}
