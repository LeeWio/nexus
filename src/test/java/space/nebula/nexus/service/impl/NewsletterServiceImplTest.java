package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import space.nebula.nexus.common.exception.BusinessException;
import space.nebula.nexus.config.NewsletterProperties;
import space.nebula.nexus.entity.Subscriber;
import space.nebula.nexus.enums.SubscriberStatus;
import space.nebula.nexus.repository.SubscriberRepository;
import space.nebula.nexus.service.IAnalyticsService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceImplTest {

	@Mock
	private SubscriberRepository subscriberRepository;
	@Mock
	private RabbitTemplate rabbitTemplate;
	@Mock
	private IAnalyticsService analyticsService;
	@Mock
	private NewsletterProperties newsletterProperties;
	@InjectMocks
	private NewsletterServiceImpl service;

	@BeforeEach
	void setUp() {
		org.mockito.Mockito.lenient().when(newsletterProperties.getBaseUrl()).thenReturn("https://nexus.example");
		org.mockito.Mockito.lenient().when(newsletterProperties.getVerificationTtl()).thenReturn(Duration.ofHours(24));
	}

	@Test
	void resubscribeNormalizesEmailAndRotatesOldTokens() {
		Subscriber subscriber = new Subscriber();
		subscriber.setEmail("user@example.com");
		subscriber.setStatus(SubscriberStatus.UNSUBSCRIBED);
		subscriber.setVerificationToken("old-verification-token");
		subscriber.setUnsubscribeToken("old-unsubscribe-token");
		when(subscriberRepository.findByEmail("user@example.com")).thenReturn(Optional.of(subscriber));

		service.subscribe(" User@Example.COM ");

		assertEquals(SubscriberStatus.PENDING, subscriber.getStatus());
		assertNotEquals("old-verification-token", subscriber.getVerificationToken());
		assertNotEquals("old-unsubscribe-token", subscriber.getUnsubscribeToken());
		assertNotNull(subscriber.getVerificationExpiresAt());
		verify(subscriberRepository).save(subscriber);
	}

	@Test
	void verifyRejectsExpiredToken() {
		Subscriber subscriber = new Subscriber();
		subscriber.setStatus(SubscriberStatus.PENDING);
		subscriber.setVerificationExpiresAt(LocalDateTime.now().minusMinutes(1));
		when(subscriberRepository.findByVerificationToken("expired")).thenReturn(Optional.of(subscriber));

		assertThrows(BusinessException.class, () -> service.verify("expired"));
	}

	@Test
	void verifyActivatesPendingSubscriptionAndConsumesToken() {
		Subscriber subscriber = new Subscriber();
		subscriber.setStatus(SubscriberStatus.PENDING);
		subscriber.setVerificationToken("valid");
		subscriber.setVerificationExpiresAt(LocalDateTime.now().plusMinutes(5));
		when(subscriberRepository.findByVerificationToken("valid")).thenReturn(Optional.of(subscriber));

		service.verify("valid");

		assertEquals(SubscriberStatus.ACTIVE, subscriber.getStatus());
		assertEquals(null, subscriber.getVerificationToken());
		assertEquals(null, subscriber.getVerificationExpiresAt());
	}

	@Test
	void subscriptionVerificationEmailLinksToTheFrontendConfirmationPage() {
		when(subscriberRepository.findByEmail("reader@example.com")).thenReturn(Optional.empty());

		service.subscribe("reader@example.com");

		var messageCaptor = org.mockito.ArgumentCaptor
				.forClass(space.nebula.nexus.payload.request.TemplateMailMessage.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(), messageCaptor.capture());
		Map<String, Object> variables = messageCaptor.getValue().getVariables();
		assertEquals(true, String.valueOf(variables.get("verifyUrl"))
				.startsWith("https://nexus.example/newsletter/verify?token="));
	}
}
