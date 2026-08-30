package space.nebula.nexus.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import java.util.List;

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

	@Test
	void audienceListFiltersByStatusAndNeverExposesSubscriptionTokens() {
		Subscriber subscriber = new Subscriber();
		subscriber.setId(42L);
		subscriber.setEmail("reader@example.com");
		subscriber.setStatus(SubscriberStatus.ACTIVE);
		subscriber.setVerificationToken("must-not-leak");
		subscriber.setUnsubscribeToken("must-not-leak");
		PageRequest pageable = PageRequest.of(0, 20);
		when(subscriberRepository.findAllByStatus(SubscriberStatus.ACTIVE, pageable))
				.thenReturn(new PageImpl<>(List.of(subscriber), pageable, 1));

		var response = service.getSubscribers(SubscriberStatus.ACTIVE, null, pageable);

		assertEquals(1, response.data().getTotal());
		assertEquals("reader@example.com", response.data().getList().getFirst().email());
		assertEquals(SubscriberStatus.ACTIVE, response.data().getList().getFirst().status());
	}

	@Test
	void audienceOverviewUsesLifecycleCountsAndRecentVerificationWindow() {
		when(subscriberRepository.countByStatus(SubscriberStatus.ACTIVE)).thenReturn(12L);
		when(subscriberRepository.countByStatus(SubscriberStatus.PENDING)).thenReturn(3L);
		when(subscriberRepository.countByStatus(SubscriberStatus.UNSUBSCRIBED)).thenReturn(4L);
		when(subscriberRepository.countByStatusAndVerifiedAtBetween(
				org.mockito.ArgumentMatchers.eq(SubscriberStatus.ACTIVE), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any())).thenReturn(5L);

		var response = service.getAudienceOverview();

		assertEquals(12, response.data().activeSubscribers());
		assertEquals(3, response.data().pendingSubscribers());
		assertEquals(4, response.data().unsubscribedSubscribers());
		assertEquals(5, response.data().verifiedLast30Days());
	}
}
