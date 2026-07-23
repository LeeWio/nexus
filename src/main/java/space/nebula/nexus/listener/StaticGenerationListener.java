package space.nebula.nexus.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import space.nebula.nexus.config.RabbitMQConfig;
import space.nebula.nexus.payload.event.StaticGenerationMessage;
import space.nebula.nexus.service.impl.StaticGenerationServiceImpl;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaticGenerationListener {

	private final StaticGenerationServiceImpl staticGenerationService;

	@RabbitListener(queues = RabbitMQConfig.STATIC_GEN_QUEUE)
	public void handleStaticGeneration(StaticGenerationMessage message) {
		log.debug("Received static generation message: {}", message);

		if (message.getAction() == StaticGenerationMessage.Action.GENERATE) {
			staticGenerationService.executeGenerate(message.getPostId());
		} else if (message.getAction() == StaticGenerationMessage.Action.DELETE) {
			staticGenerationService.executeDelete(message.getSlug());
		}
	}
}
