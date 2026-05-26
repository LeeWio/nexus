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
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import space.nebula.nexus.common.event.CommentSubmittedEvent;
import space.nebula.nexus.common.event.PostChangedEvent;
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

	@Async("asyncExecutor")
	@EventListener
	public void handlePostChangedEvent(PostChangedEvent event)
	{
		if (event.isNew())
		{
			dispatchToSubscribers(WebhookEvent.POST_PUBLISHED,
					Dict.create().set("postId", event.getPost().getId()).set("title", event.getPost().getTitle())
							.set("slug", event.getPost().getSlug()).set("status", event.getPost().getStatus().name()));
		}
	}

	@Async("asyncExecutor")
	@EventListener
	public void handlePostDeletedEvent(PostDeletedEvent event)
	{
		dispatchToSubscribers(WebhookEvent.POST_DELETED,
				Dict.create().set("postId", event.getPostId()).set("slug", event.getSlug()));
	}

	@Async("asyncExecutor")
	@EventListener
	public void handleCommentSubmittedEvent(CommentSubmittedEvent event)
	{
		dispatchToSubscribers(WebhookEvent.COMMENT_SUBMITTED, Dict.create().set("commentId", event.getComment().getId())
				.set("author", event.getComment().getUser().getUsername())
				.set("content", event.getComment().getContent()).set("status", event.getComment().getStatus().name()));
	}

	private void dispatchToSubscribers(WebhookEvent eventType, Dict payload)
	{
		List<Webhook> activeWebhooks = webhookRepository.findAllByIsActiveTrue();

		for (Webhook webhook : activeWebhooks)
		{
			List<WebhookEvent> subscribedEvents = webhookMapper.stringToEvents(webhook.getEvents());
			if (CollUtil.contains(subscribedEvents, eventType))
			{
				payload.set("event", eventType.name());
				payload.set("timestamp", System.currentTimeMillis());
				dispatchPayload(webhook, payload);
			}
		}
	}

	@Async("asyncExecutor")
	public void dispatchPayload(Webhook webhook, Dict payload)
	{
		String jsonPayload = JSONUtil.toJsonStr(payload);

		// Sign the payload using HMAC-SHA256
		HMac mac = SecureUtil.hmac(HmacAlgorithm.HmacSHA256, webhook.getSecret().getBytes());
		String signature = mac.digestHex(jsonPayload);

		try (HttpResponse response = HttpRequest.post(webhook.getUrl()).header("Content-Type", "application/json")
				.header("X-Nexus-Signature", signature).header("X-Nexus-Event", payload.getStr("event"))
				.body(jsonPayload).timeout(5000).execute())
		{

			if (response.isOk())
			{
				log.debug("Successfully dispatched webhook {} to URL: {}", payload.getStr("event"), webhook.getUrl());
			}
			else
			{
				log.warn("Webhook dispatch failed for {} to URL: {}. HTTP Status: {}", payload.getStr("event"),
						webhook.getUrl(), response.getStatus());
			}
		}
		catch (Exception e)
		{
			log.error("Exception occurred while dispatching webhook {} to URL: {}", payload.getStr("event"),
					webhook.getUrl(), e);
		}
	}
}
