package space.nebula.nexus.listener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import space.nebula.nexus.payload.request.TemplateMailMessage;
import space.nebula.nexus.service.NotificationDeliveryService;
import space.nebula.nexus.service.NotificationDeliveryService.DeliveryClaim;
import space.nebula.nexus.service.NewsletterDeliveryService;
import space.nebula.nexus.utils.MailUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MailMessageListenerTest {
	@Mock
	private MailUtil mailUtil;
	@Mock
	private NotificationDeliveryService deliveries;
	@Mock
	private NewsletterDeliveryService newsletters;
	@InjectMocks
	private MailMessageListener listener;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(listener, "applicationBaseUrl", "https://example.com");
	}

	@Test
	void duplicateNotificationMessageDoesNotSendMail() {
		when(deliveries.beginDelivery(31L)).thenReturn(Optional.empty());
		listener.processMailMessage(notification());
		verifyNoInteractions(mailUtil, newsletters);
	}

	@Test
	void notificationUsesPersistedPayloadAndAbsoluteApplicationLink() {
		when(deliveries.beginDelivery(31L)).thenReturn(Optional.of(claim("/post/example")));
		listener.processMailMessage(notification());
		verify(mailUtil).sendSimpleMail("reader@example.com", "Odyssey - New reply",
				"A reply arrived.\n\nOpen in Odyssey: https://example.com/post/example");
		verify(deliveries).markDelivered(31L, 2);
		verifyNoInteractions(newsletters);
	}

	@Test
	void smtpFailureIsRecordedWithoutThrowingForRabbitRetry() {
		when(deliveries.beginDelivery(31L)).thenReturn(Optional.of(claim(null)));
		IllegalStateException error = new IllegalStateException("SMTP unavailable");
		doThrow(error).when(mailUtil).sendSimpleMail(anyString(), anyString(), anyString());
		assertDoesNotThrow(() -> listener.processMailMessage(notification()));
		verify(deliveries).markFailed(31L, 2, error);
		verify(deliveries, never()).markDelivered(anyLong(), anyInt());
	}

	@Test
	void externalLinkCannotBeDeliveredAsApplicationLink() {
		when(deliveries.beginDelivery(31L)).thenReturn(Optional.of(claim("//other.example/path")));
		listener.processMailMessage(notification());
		verifyNoInteractions(mailUtil);
		verify(deliveries).markFailed(eq(31L), eq(2), isA(IllegalArgumentException.class));
	}

	@Test
	void legacyNewsletterStillUsesItsExistingMailAndTrackingPath() {
		TemplateMailMessage message = TemplateMailMessage.builder().to("reader@example.com").subject("Newsletter")
				.content("News").type(TemplateMailMessage.MailType.SIMPLE).newsletterDeliveryId(41L).build();
		listener.processMailMessage(message);
		verify(mailUtil).sendSimpleMail("reader@example.com", "Newsletter", "News");
		verify(newsletters).markDelivered(41L);
		verifyNoInteractions(deliveries);
	}

	private DeliveryClaim claim(String link) {
		return new DeliveryClaim(31L, 2, "reader@example.com", "New reply", "A reply arrived.", link);
	}
	private TemplateMailMessage notification() {
		return TemplateMailMessage.builder().notificationDeliveryId(31L).build();
	}
}
