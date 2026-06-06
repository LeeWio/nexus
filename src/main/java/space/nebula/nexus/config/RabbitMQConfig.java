package space.nebula.nexus.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig
{

	public static final String CANAL_EXCHANGE = "canal.exchange";
	public static final String CANAL_ROUTING_KEY = "canal.routing.key";
	public static final String CANAL_QUEUE = "nexus.cache.invalidation.queue";

	public static final String MAIL_EXCHANGE = "nexus.mail.exchange";
	public static final String MAIL_ROUTING_KEY = "nexus.mail.routing.key";
	public static final String MAIL_QUEUE = "nexus.mail.send.queue";

	public static final String WEBHOOK_EXCHANGE = "nexus.webhook.exchange";
	public static final String WEBHOOK_ROUTING_KEY = "nexus.webhook.routing.key";
	public static final String WEBHOOK_QUEUE = "nexus.webhook.dispatch.queue";

	public static final String CACHE_BROADCAST_EXCHANGE = "nexus.cache.broadcast.exchange";

	@Bean
	MessageConverter jsonMessageConverter()
	{
		return new JacksonJsonMessageConverter();
	}

	@Bean
	DirectExchange canalExchange()
	{
		return new DirectExchange(CANAL_EXCHANGE);
	}

	@Bean
	Queue canalQueue()
	{
		return new Queue(CANAL_QUEUE, true);
	}

	@Bean
	Binding canalBinding(Queue canalQueue, DirectExchange canalExchange)
	{
		return BindingBuilder.bind(canalQueue).to(canalExchange).with(CANAL_ROUTING_KEY);
	}

	@Bean
	FanoutExchange cacheBroadcastExchange()
	{
		return new FanoutExchange(CACHE_BROADCAST_EXCHANGE);
	}

	/**
	 * Unique queue for each instance to receive cache invalidation broadcasts.
	 * Using auto-delete so it cleans up when the instance goes down.
	 */
	@Bean
	Queue l1CacheInvalidationQueue()
	{
		return new Queue("nexus.l1.invalidation." + cn.hutool.core.util.IdUtil.fastSimpleUUID(), false, false, true);
	}

	@Bean
	Binding l1CacheBinding(Queue l1CacheInvalidationQueue, FanoutExchange cacheBroadcastExchange)
	{
		return BindingBuilder.bind(l1CacheInvalidationQueue).to(cacheBroadcastExchange);
	}

	@Bean
	DirectExchange mailExchange()
	{
		return new DirectExchange(MAIL_EXCHANGE);
	}

	@Bean
	Queue mailQueue()
	{
		return new Queue(MAIL_QUEUE, true);
	}

	@Bean
	Binding mailBinding(Queue mailQueue, DirectExchange mailExchange)
	{
		return BindingBuilder.bind(mailQueue).to(mailExchange).with(MAIL_ROUTING_KEY);
	}

	@Bean
	DirectExchange webhookExchange()
	{
		return new DirectExchange(WEBHOOK_EXCHANGE);
	}

	@Bean
	Queue webhookQueue()
	{
		return new Queue(WEBHOOK_QUEUE, true);
	}

	@Bean
	Binding webhookBinding(Queue webhookQueue, DirectExchange webhookExchange)
	{
		return BindingBuilder.bind(webhookQueue).to(webhookExchange).with(WEBHOOK_ROUTING_KEY);
	}
}
