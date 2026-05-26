package space.nebula.nexus.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
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

	@Bean
	public MessageConverter jsonMessageConverter()
	{
		return new JacksonJsonMessageConverter();
	}

	@Bean
	public DirectExchange canalExchange()
	{
		return new DirectExchange(CANAL_EXCHANGE);
	}

	@Bean
	public Queue canalQueue()
	{
		return new Queue(CANAL_QUEUE, true);
	}

	@Bean
	public Binding canalBinding(Queue canalQueue, DirectExchange canalExchange)
	{
		return BindingBuilder.bind(canalQueue).to(canalExchange).with(CANAL_ROUTING_KEY);
	}

	@Bean
	public DirectExchange mailExchange()
	{
		return new DirectExchange(MAIL_EXCHANGE);
	}

	@Bean
	public Queue mailQueue()
	{
		return new Queue(MAIL_QUEUE, true);
	}

	@Bean
	public Binding mailBinding(Queue mailQueue, DirectExchange mailExchange)
	{
		return BindingBuilder.bind(mailQueue).to(mailExchange).with(MAIL_ROUTING_KEY);
	}
}
