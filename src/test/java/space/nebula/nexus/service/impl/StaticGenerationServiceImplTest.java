package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import space.nebula.nexus.common.storage.StorageProvider;
import space.nebula.nexus.entity.Post;
import space.nebula.nexus.payload.event.StaticGenerationMessage;
import space.nebula.nexus.repository.PostRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import space.nebula.nexus.config.RabbitMQConfig;

import java.io.InputStream;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticGenerationServiceImplTest {

	@Mock
	private TemplateEngine templateEngine;
	@Mock
	private StorageProvider storageProvider;
	@Mock
	private PostRepository postRepository;
	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private StaticGenerationServiceImpl staticGenerationService;

	@Test
	void generatePostStaticHtml_DispatchesMessage() {
		Post post = new Post();
		post.setId(1L);
		post.setSlug("test-post");

		staticGenerationService.generatePostStaticHtml(post);

		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.STATIC_GEN_EXCHANGE),
				eq(RabbitMQConfig.STATIC_GEN_ROUTING_KEY), any(StaticGenerationMessage.class));
	}

	@Test
	void deletePostStaticHtml_DispatchesMessage() {
		staticGenerationService.deletePostStaticHtml("test-post");

		verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.STATIC_GEN_EXCHANGE),
				eq(RabbitMQConfig.STATIC_GEN_ROUTING_KEY), any(StaticGenerationMessage.class));
	}

	@Test
	void executeGenerate_Success() {
		Post post = new Post();
		post.setId(1L);
		post.setSlug("test-post");

		when(postRepository.findById(1L)).thenReturn(Optional.of(post));
		when(templateEngine.process(eq("post-static"), any(Context.class))).thenReturn("<html></html>");

		staticGenerationService.executeGenerate(1L);

		verify(storageProvider).store(any(InputStream.class), eq("static/posts/test-post.html"));
	}

	@Test
	void executeDelete_Success() {
		staticGenerationService.executeDelete("test-post");
		verify(storageProvider).delete(eq("static/posts/test-post.html"));
	}
}
